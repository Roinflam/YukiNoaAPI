package pers.tany.yukinoaapi.interfacepart.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * {@link IConfig#saveConfigAsync} 的实现：调用线程上拍快照，后台单线程做 YAML 序列化与写盘。
 *
 * <p>为什么要这样拆：大数据文件的 {@code FileConfiguration.save} 几乎全部耗时都在 YAML 序列化
 * （{@code toNodeTree} + {@code Yaml.serialize}），在主线程上跑就是一次卡顿（文件大时一次能到上百毫秒）。
 * 快照只是把树拷一遍，便宜得多；序列化和写盘挪到后台后，主线程不再等。</p>
 *
 * <p>快照与原文件<b>读回等价</b>的保证（常见数据在 1.12.2 / 1.17.1 / 1.20.1 上实测写出的文本也逐字相同）：</p>
 * <ul>
 *   <li>按 {@code getValues(false)} 逐层拷贝——与 {@code YamlConfiguration.saveToString} 读取数据的方式相同（含 copyDefaults 时的默认值）；</li>
 *   <li>{@link ConfigurationSerializable}（物品、坐标等，包括放在 List / 数组 / Map 里的）在调用线程上就展开成
 *       「{@code ==}: 别名 + serialize() 的内容」的 Map，这正是 YAML 表示器写出它们时用的结构，后台线程不会去碰这些对象；
 *       对象数组展开成 List（与 SnakeYAML 写数组的形式相同），基本类型数组拷一份；认不出的类型直接拒绝（调用方退回同步保存），不与后台共享；</li>
 *   <li>1.18.1+ 的注释、文件头/尾、缩进、宽度、解析注释开关用反射原样拷贝（本库按 1.17 API 编译，旧版服务端上这些 API 不存在就跳过）；
 *       旧版上 copyHeader 时取默认配置的文件头，与原版 buildHeader 一致；</li>
 *   <li>与原文本的已知差异：同一个 List 实例在配置里出现两次时，原版写成 YAML 别名（id001 锚点），快照写成两份展开的文本，读回相同。</li>
 * </ul>
 *
 * <p>一致性：同一文件连续提交时，后台只写最新那份；{@link IConfig#saveConfig} 同步保存时会作废排队中的旧快照，
 * 并与后台写入共用同一把文件锁（后台正在写同一个文件时，同步保存会在调用线程上等它写完）。
 * 写入先落临时文件再替换；Windows 上目标文件被别的程序打开时原子替换会失败，重试几次后退回直接覆盖写。</p>
 *
 * <p>资源：锁表只为用过异步保存的文件建条目，序号写完即释放；执行器线程空闲 60 秒自动退出（插件重载后不残留）。</p>
 */
final class AsyncConfigSaver {

    private AsyncConfigSaver() {
    }

    /** 每个文件（绝对路径）最近一次提交的序号；后台任务发现自己不是最新的就跳过 */
    private static final Map<String, Long> LATEST = new ConcurrentHashMap<>();
    /** 每个文件一把锁：同步保存与后台写入互斥 */
    private static final Map<String, Object> LOCKS = new ConcurrentHashMap<>();
    private static final AtomicLong SEQ = new AtomicLong();

    private static volatile ThreadPoolExecutor executor;

    // ==================== 1.18.1+ 才有的 API（反射，缺失时为 null） ====================
    private static final Method GET_COMMENTS = method(ConfigurationSection.class, "getComments", String.class);
    private static final Method SET_COMMENTS = method(ConfigurationSection.class, "setComments", String.class, List.class);
    private static final Method GET_INLINE_COMMENTS = method(ConfigurationSection.class, "getInlineComments", String.class);
    private static final Method SET_INLINE_COMMENTS = method(ConfigurationSection.class, "setInlineComments", String.class, List.class);

    private static Method method(Class<?> owner, String name, Class<?>... params) {
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ==================== 对外入口 ====================

    /**
     * 拍快照并排队写盘。快照失败（例如遇到认不出的值类型）会抛异常，由调用方退回同步保存。
     */
    static void save(Plugin plugin, FileConfiguration config, File file, boolean verboseLog) {
        final YamlConfiguration snapshot = snapshot(config);
        final String key = key(file);
        final Object lock = LOCKS.computeIfAbsent(key, k -> new Object());
        final long id = SEQ.incrementAndGet();
        LATEST.put(key, id);
        Runnable task = () -> {
            synchronized (lock) {
                Long latest = LATEST.get(key);
                if (latest == null || latest != id) {
                    return; // 已有更新的快照（或同步保存）排在后面
                }
                try {
                    writeAtomically(snapshot, file);
                    if (verboseLog) {
                        plugin.getLogger().info("成功保存配置文件（异步）: " + file.getAbsolutePath());
                    }
                } catch (Throwable t) {
                    plugin.getLogger().log(Level.WARNING, "异步保存配置文件时发生错误: " + file.getAbsolutePath(), t);
                } finally {
                    LATEST.remove(key, id);
                }
            }
        };
        try {
            executor().execute(task);
        } catch (RejectedExecutionException e) {
            // 执行器不可用：退回在调用线程上直接写
            task.run();
        }
    }

    /**
     * 同步保存用：这个文件用过异步保存才有锁，否则返回 null（同步保存直接写，不建任何条目）。
     */
    static Object existingLock(File file) {
        return LOCKS.get(key(file));
    }

    /**
     * 同步保存用：作废这个文件在排队中的异步快照（持锁调用），返回作废用的序号，写完后交给 {@link #release}。
     */
    static long supersede(File file) {
        long id = SEQ.incrementAndGet();
        LATEST.put(key(file), id);
        return id;
    }

    /** 同步保存写完后释放作废序号（期间若又有新的异步快照提交，序号已变，不会误删） */
    static void release(File file, long id) {
        LATEST.remove(key(file), id);
    }

    /**
     * 等排队中的异步写入全部完成（最多 timeoutMillis）。执行器是单线程先进先出，
     * 排一个空任务进去等它跑完，就说明之前提交的都写完了。
     */
    static void flush(long timeoutMillis) {
        ThreadPoolExecutor ex = executor;
        if (ex == null) {
            return;
        }
        try {
            ex.submit(() -> { }).get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // 超时或执行器已关闭：尽力而为
        }
    }

    // ==================== 内部实现 ====================

    /** 同一文件的规范化键（避免 .. 与相对路径让同一文件拿到两把锁） */
    private static String key(File file) {
        return file.toPath().toAbsolutePath().normalize().toString();
    }

    private static ThreadPoolExecutor executor() {
        ThreadPoolExecutor ex = executor;
        if (ex == null) {
            synchronized (AsyncConfigSaver.class) {
                ex = executor;
                if (ex == null) {
                    // 单线程、先进先出；空闲 60 秒线程自动退出，插件重载后不会留下空转线程钉住旧的类加载器
                    ex = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), r -> {
                        Thread t = new Thread(r, "YukiNoaAPI-ConfigSaver");
                        t.setDaemon(true);
                        return t;
                    });
                    ex.allowCoreThreadTimeOut(true);
                    executor = ex;
                }
            }
        }
        return ex;
    }

    private static void writeAtomically(YamlConfiguration snapshot, File file) throws Exception {
        File dir = file.getAbsoluteFile().getParentFile();
        if (dir != null) {
            dir.mkdirs();
        }
        File tmp = new File(dir, file.getName() + ".saving.tmp");
        // 用 FileConfiguration.save 写临时文件：字符集等细节与原来的同步保存完全一致
        snapshot.save(tmp);
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                try {
                    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (IOException e) {
                // Windows：目标文件被别的程序（备份工具、编辑器）打开时替换会失败，稍等重试
                last = e;
                Thread.sleep(50L * (attempt + 1));
            }
        }
        // 仍然失败：退回直接覆盖写（与旧的同步保存同一种写法，对方只读打开时也能成功），最后删掉临时文件
        try {
            Files.copy(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (last != null) {
                e.addSuppressed(last);
            }
            throw e;
        } finally {
            Files.deleteIfExists(tmp.toPath());
        }
    }

    private static YamlConfiguration snapshot(FileConfiguration src) {
        YamlConfiguration dst = new YamlConfiguration();
        copyOptions(src, dst);
        copySection(src, dst);
        return dst;
    }

    private static void copySection(ConfigurationSection from, ConfigurationSection to) {
        for (Map.Entry<String, Object> e : from.getValues(false).entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (value instanceof ConfigurationSection) {
                copySection((ConfigurationSection) value, to.createSection(key));
            } else {
                to.set(key, detach(value));
            }
            copyComments(from, to, key);
        }
    }

    /**
     * 把值拷成与原对象脱钩、只含基础类型/List/Map 的结构。
     * 字符串、数字、布尔本身不可变，直接复用。
     */
    @SuppressWarnings("unchecked")
    private static Object detach(Object value) {
        if (value == null || value instanceof String || value instanceof Number
                || value instanceof Boolean || value instanceof Character
                || value instanceof UUID || value instanceof Enum) {
            return value;
        }
        if (value instanceof ConfigurationSerializable) {
            // 与 YAML 表示器写出 ConfigurationSerializable 的结构相同：先写 "==": 别名，再写 serialize() 的内容
            ConfigurationSerializable s = (ConfigurationSerializable) value;
            Map<String, Object> out = new LinkedHashMap<>();
            out.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(s.getClass()));
            for (Map.Entry<String, Object> e : s.serialize().entrySet()) {
                out.put(e.getKey(), detach(e.getValue()));
            }
            return out;
        }
        if (value instanceof ConfigurationSection) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((ConfigurationSection) value).getValues(false).entrySet()) {
                out.put(e.getKey(), detach(e.getValue()));
            }
            return out;
        }
        if (value instanceof Object[]) {
            // 例如 inventory.getContents() 存进来的 ItemStack[]：元素是活物品的镜像，必须在这里展开，不能交给后台
            Object[] arr = (Object[]) value;
            List<Object> out = new ArrayList<>(arr.length);
            for (Object o : arr) {
                out.add(detach(o));
            }
            return out;
        }
        if (value.getClass().isArray()) {
            // 基本类型数组：拷一份、保持原类型（byte[] 仍写成 !!binary）
            int n = Array.getLength(value);
            Object copy = Array.newInstance(value.getClass().getComponentType(), n);
            System.arraycopy(value, 0, copy, 0, n);
            return copy;
        }
        if (value instanceof List) {
            List<Object> out = new ArrayList<>(((List<?>) value).size());
            for (Object o : (List<?>) value) {
                out.add(detach(o));
            }
            return out;
        }
        if (value instanceof Set) {
            Set<Object> out = new LinkedHashSet<>();
            for (Object o : (Set<?>) value) {
                out.add(detach(o));
            }
            return out;
        }
        if (value instanceof Collection) {
            List<Object> out = new ArrayList<>();
            for (Object o : (Collection<?>) value) {
                out.add(detach(o));
            }
            return out;
        }
        if (value instanceof Map) {
            Map<Object, Object> out = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> e : ((Map<Object, Object>) value).entrySet()) {
                out.put(e.getKey(), detach(e.getValue()));
            }
            return out;
        }
        // 认不出的类型：不与后台线程共享，拒绝异步（saveConfigAsync 会退回同步保存）
        throw new IllegalArgumentException("无法为异步保存拍快照的值类型: " + value.getClass().getName());
    }

    private static void copyComments(ConfigurationSection from, ConfigurationSection to, String key) {
        if (GET_COMMENTS == null || SET_COMMENTS == null) {
            return;
        }
        try {
            List<?> comments = (List<?>) GET_COMMENTS.invoke(from, key);
            if (comments != null && !comments.isEmpty()) {
                SET_COMMENTS.invoke(to, key, new ArrayList<Object>(comments));
            }
            if (GET_INLINE_COMMENTS != null && SET_INLINE_COMMENTS != null) {
                List<?> inline = (List<?>) GET_INLINE_COMMENTS.invoke(from, key);
                if (inline != null && !inline.isEmpty()) {
                    SET_INLINE_COMMENTS.invoke(to, key, new ArrayList<Object>(inline));
                }
            }
        } catch (Throwable ignored) {
            // 注释只影响可读性，拷不过去不影响数据
        }
    }

    private static void copyOptions(FileConfiguration src, YamlConfiguration dst) {
        Object so = src.options();
        Object dopt = dst.options();
        dst.options().pathSeparator(src.options().pathSeparator());
        dst.options().copyDefaults(src.options().copyDefaults());
        dst.options().copyHeader(src.options().copyHeader());
        if (src instanceof YamlConfiguration) {
            dst.options().indent(((YamlConfiguration) src).options().indent());
        }
        // 1.18.1+：列表形式的文件头/尾、宽度、是否解析注释；旧版只有字符串形式的 header()
        boolean listHeaderCopied = invokeCopy(so, dopt, "getHeader", "setHeader", List.class);
        if (!listHeaderCopied) {
            // 旧版（1.17 及更早）：原版 buildHeader 在 copyHeader 时优先用默认配置的文件头
            String header = src.options().header();
            if (src.options().copyHeader() && src.getDefaults() instanceof FileConfiguration) {
                String defHeader = ((FileConfiguration) src.getDefaults()).options().header();
                if (defHeader != null && !defHeader.isEmpty()) {
                    header = defHeader;
                }
            }
            dst.options().header(header);
        }
        invokeCopy(so, dopt, "getFooter", "setFooter", List.class);
        invokeCopy(so, dopt, "width", "width", int.class);
        invokeCopy(so, dopt, "parseComments", "parseComments", boolean.class);
    }

    private static boolean invokeCopy(Object from, Object to, String getter, String setter, Class<?> type) {
        try {
            Method g = from.getClass().getMethod(getter);
            Method s = to.getClass().getMethod(setter, type);
            Object v = g.invoke(from);
            if (v instanceof List) {
                v = new ArrayList<Object>((List<?>) v);
            }
            s.invoke(to, v);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
