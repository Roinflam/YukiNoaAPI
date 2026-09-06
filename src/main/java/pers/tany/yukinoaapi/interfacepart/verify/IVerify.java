package pers.tany.yukinoaapi.interfacepart.verify;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.LogUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 插件授权验证接口(含挑战-响应签名机制 + 离线宽限期)
 *
 * <p>向 FlameForge 验证服务器(默认 https://verify.flameforge.tech/)发送验证请求。
 * 后端以「IP 前三段 + 插件名」作为客户端身份,机器码仅作记录展示不参与匹配。</p>
 *
 * <h3>白名单 IP 免验证码机制</h3>
 * <p>后端会先检查请求来源 IP 是否在白名单中(配置文件 + 数据库白名单),
 * 命中则直接放行,无需验证码即可通过。因此<b>客户端不会在本地拦截"验证码为空"</b>,
 * 而是将请求发到服务端由服务端裁决。这样开发者自己的服务器、合作伙伴的服务器
 * 配进白名单后,VerifyCode 字段填空字符串也能正常启用插件。</p>
 *
 * <h3>挑战-响应签名机制</h3>
 * <p>为防止"假服务器伪造响应"攻击(例如 hosts 劫持把 verify.flameforge.tech 指向
 * 攻击者自己的服务器,然后返回 isValid=true),客户端实施以下流程:
 * <ol>
 *   <li>每次请求生成随机 nonce(一次性)并携带在请求中</li>
 *   <li>服务端对 {@code pluginName|code|machineCode|nonce|isValid|timestamp} 用私钥签名</li>
 *   <li>客户端用预置的公钥({@link #PUBLIC_KEY_BASE64})验证签名</li>
 *   <li>同时校验 nonce 回传一致、timestamp 在 ±5 分钟内</li>
 *   <li>任一校验失败 → 立即认为是假服务器,禁用插件</li>
 * </ol>
 * </p>
 *
 * <h3>离线宽限期机制(v3.2.1 新增)</h3>
 * <p>验证服务器故障(TLS 证书链问题、机房断网、Nginx 502 等)不应该让所有客户的
 * 服务器同时无法启动。为此客户端增加了离线宽限:</p>
 * <ol>
 *   <li><b>写缓存</b>:每次在线验证通过(且签名校验通过)后,把服务端签名过的
 *       待签内容与签名一起落盘到 {@code plugins/YukiNoaAPI/verify-cache/&lt;插件名&gt;.token}</li>
 *   <li><b>用缓存</b>:仅当"网络异常 / 服务端 5xx / 429"时,读取该缓存,
 *       用同一个公钥校验签名,并校验:
 *       <ul>
 *         <li>缓存中的 {@code pluginName|code|machineCode} 前缀与当前环境完全一致</li>
 *         <li>缓存中的 {@code isValid} 为 true</li>
 *         <li>缓存中的签名时间戳距今不超过 {@link #GRACE_PERIOD_SECONDS}(7 天)</li>
 *       </ul>
 *       全部通过 → 放行并打 WARN;任一不通过 → 按原逻辑禁用插件</li>
 *   <li><b>清缓存</b>:服务端明确返回 isValid=false(授权被吊销)且签名有效时,
 *       立刻删除本地缓存,避免吊销后仍能靠宽限期继续使用</li>
 * </ol>
 *
 * <p><b>为什么这样做不会削弱防盗版强度:</b>缓存内容由你的 RSA 私钥签名,
 * 服主既无法伪造也无法篡改(改一个字节签名即失效);机器码 + 插件名 + 验证码
 * 三者绑定在签名内容里,缓存文件复制到别的机器上前缀比对就会失败;
 * 宽限窗口由服务端签发的时间戳约束,最长 7 天,且宽限天数是编译期常量,
 * <b>不从 config.yml 读取</b>,服主无法自行延长。</p>
 *
 * <p><b>已知局限:</b>服主若把系统时钟往回调,可以让缓存看起来一直"很新"。
 * 这属于本地时钟不可信的固有问题,如需彻底解决需要引入可信时间源,当前不做。</p>
 *
 * <h3>公钥配置</h3>
 * <p><b>首次部署必做:</b>运行一次后端(签名服务会自动生成密钥对),
 * 在后端日志或 {@code public-key.txt} 文件里复制公钥,
 * 替换下方 {@link #PUBLIC_KEY_BASE64} 常量,然后重新编译 YukiNoaAPI。</p>
 *
 * <p>若未配置(保持占位符),签名验证会被跳过(开发模式),
 * 控制台输出明显的警告日志,便于你在本地调试。
 * <b>注意:占位符模式下离线宽限一并禁用</b>,因为没有公钥无法校验缓存真伪。</p>
 *
 * <h3>机器码采集策略(多因子指纹)</h3>
 * <p>依次尝试收集 MAC / 主机名 / 用户目录 / OS / CPU / 账户名,任一项失败不影响整体。
 * 输出格式:{@code osType-hostPrefix-hash16}(例:{@code linux-mohost01-a3f9c8d2e1b4f678})。</p>
 *
 * <h3>调用方式</h3>
 * <pre>{@code
 * @Override
 * public void onEnable() {
 *     saveDefaultConfig();
 *     String code = getConfig().getString("VerifyCode", "");
 *     if (!IVerify.verifyOrDisable(this, getName(), code)) return;
 * }
 * }</pre>
 *
 * @author Roinflam
 * @since 3.2.0
 */
public final class IVerify {

    // ========================================================================
    // ⚠ 部署配置项 - 首次部署请修改此处
    // ========================================================================

    /**
     * 服务端 RSA 公钥(X.509 SubjectPublicKeyInfo,Base64)
     *
     * <p><b>请在首次部署时替换为后端生成的真实公钥</b>:
     * 启动后端后,公钥会打印在日志中并保存到 {@code public-key.txt} 文件,
     * 复制该 Base64 字符串替换下方占位符,然后重新编译 YukiNoaAPI。</p>
     *
     * <p>占位符模式下签名验证会被跳过,仅用于本地开发调试。</p>
     */
    private static final String PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuDGma7PFCxmFmbQTvNeb3H9MX4KHkn5COYg67t3wRjxZMjLyY2mOFXjlWNLlwBZND0upAmXylkpcDtNR8ux25UtfNKyyDy/oe6hvlkER8w/V79WYgRWzyJlwrwXaA/gI2XRMU8zeh6MpMmLRsNm7KS6ug/MnpgOuH6BqU1pRUusUm7skSR1IsYzwQHDtveOzlx5hi+bvPAYnleQ8O7uoVOAtIEBzbSzcECgjn4d7qkxudYdAKcMm7v1vFfwWvBC1fEh7+9aReonhE+drzFCUntqp6taUYt7Ft/Vvk5Pyj70mfiEe/wTUNpruu/HRJFWfmcFtv7T5t/No1IAOL1VVsQIDAQAB";

    /** 占位符值(用于判断是否已完成公钥配置) */
    private static final String PUBLIC_KEY_PLACEHOLDER = "PASTE_YOUR_PUBLIC_KEY_HERE";

    /** 默认验证端点;可通过 YukiNoaAPI 的 config.yml 中 {@code Verify.ServerUrl} 覆盖 */
    private static final String DEFAULT_URL = "https://verify.flameforge.tech/";

    /** 时间戳允许的最大漂移(秒,防录制重放) */
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300L;

    /**
     * 离线宽限期长度(秒,默认 7 天)
     *
     * <p><b>刻意写成编译期常量,不从 config.yml 读取</b>:
     * 否则服主可以把它改成 999999 天,等价于永久破解。</p>
     */
    private static final long GRACE_PERIOD_SECONDS = 7L * 24L * 60L * 60L;

    // ========================================================================
    // 内部常量
    // ========================================================================

    /** 连接超时(毫秒) */
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    /** 读取超时(毫秒) */
    private static final int READ_TIMEOUT_MS = 10_000;

    /** 网络失败重试次数(总请求次数 = MAX_RETRY + 1) */
    private static final int MAX_RETRY = 1;

    /** 机器码字段最大长度 */
    private static final int MACHINE_CODE_MAX_LEN = 60;

    /** nonce 长度(16 字节 → Base64URL 约 22 字符) */
    private static final int NONCE_BYTES = 16;

    /** 离线缓存目录名(位于 YukiNoaAPI 数据目录下) */
    private static final String CACHE_DIR_NAME = "verify-cache";

    /** 离线缓存文件后缀 */
    private static final String CACHE_FILE_SUFFIX = ".token";

    /** 离线缓存格式版本(格式变更时用于拒绝旧缓存) */
    private static final String CACHE_VERSION = "1";

    /** 缓存文件名最大长度(防超长插件名) */
    private static final int CACHE_FILE_NAME_MAX_LEN = 48;

    // ========================================================================
    // JSON 简易解析正则(避免引入 gson 依赖)
    // ========================================================================

    /** 匹配 success / isValid 字段 */
    private static final Pattern ISVALID_PATTERN = Pattern.compile(
            "\"(?:success|isValid)\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);

    /** 匹配 message 字段(支持转义) */
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(
            "\"message\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    /** 匹配 nonce 字段 */
    private static final Pattern NONCE_PATTERN = Pattern.compile(
            "\"nonce\"\\s*:\\s*\"([A-Za-z0-9_\\-]+)\"");

    /** 匹配 signature 字段(Base64 字符集 + 转义) */
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile(
            "\"signature\"\\s*:\\s*\"([A-Za-z0-9+/=]+)\"");

    /** 匹配 timestamp 字段(数字) */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "\"timestamp\"\\s*:\\s*(\\d+)");

    /** 机器码进程内缓存 */
    private static volatile String cachedMachineCode;

    /** 密码学安全随机数(用于 nonce 生成) */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 工具类不允许实例化 */
    private IVerify() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ========================================================================
    // 公开 API
    // ========================================================================

    /**
     * 核心入口:执行验证,失败则调度下一 tick 禁用插件
     *
     * <p>对验证码字段的处理:本地不再拦截"验证码为空",而是把空字符串原样发到后端,
     * 由后端的 IP 白名单逻辑决定是否放行。这是为了让在白名单内的服务器
     * (开发者自用、合作伙伴等)无需填写验证码也能正常启用插件。</p>
     *
     * <p>若在线验证因网络/服务端故障失败,会自动尝试离线宽限期(见类注释)。</p>
     *
     * @param plugin     调用方插件实例
     * @param pluginName 插件名(建议使用 {@code plugin.getName()} 自动取 plugin.yml 的 name 字段)
     * @param code       验证码(可为空,空字符串时仅在 IP 白名单内能通过后端校验)
     * @return true 通过,false 失败(此时已调度禁用任务)
     */
    public static boolean verifyOrDisable(JavaPlugin plugin, String pluginName, String code) {
        if (plugin == null) {
            return false;
        }
        if (pluginName == null || pluginName.trim().isEmpty()) {
            bannerAndDisable(plugin, "[FlameForge-Verify] 插件名为空");
            return false;
        }

        // 占位符模式警告(仅开发/测试用,分发给客户前务必配置真实公钥)
        if (isPublicKeyNotConfigured()) {
            LogUtil.warning("[FlameForge-Verify] ⚠ PUBLIC_KEY_BASE64 未配置,签名验证被跳过(开发模式)");
            LogUtil.warning("[FlameForge-Verify] ⚠ 分发插件前请务必将后端生成的公钥填入 IVerify.java");
        }

        // 验证码空处理:不本地拦截,原样发给后端
        // 后端会优先检查 IP 白名单,命中则放行;否则才进行验证码合法性校验
        String trimmedCode = (code == null) ? "" : code.trim();

        VerifyResult result = doVerify(plugin, pluginName.trim(), trimmedCode);
        if (result.success) {
            LogUtil.success("[" + plugin.getName() + "] 授权验证通过 (plugin=" + pluginName
                    + (result.offlineGrace ? ", 离线宽限模式" : "") + ")");
            return true;
        }

        bannerAndDisable(plugin,
                "[FlameForge-Verify] " + (result.message != null ? result.message : "验证失败"));
        return false;
    }

    /**
     * 获取当前机器的指纹(多因子哈希)
     *
     * @return 机器码字符串
     */
    public static String getMachineCode() {
        String cached = cachedMachineCode;
        if (cached != null) {
            return cached;
        }
        synchronized (IVerify.class) {
            if (cachedMachineCode != null) {
                return cachedMachineCode;
            }
            cachedMachineCode = buildMachineCode();
            return cachedMachineCode;
        }
    }

    // ========================================================================
    // 验证请求实现(含挑战-响应签名验证 + 离线宽限兜底)
    // ========================================================================

    /**
     * 执行 HTTP 验证请求,含重试;全部失败时回退到离线宽限
     *
     * <p>关键区分:只有<b>传输层/服务端故障</b>(网络异常、HTTP 5xx、HTTP 429)
     * 才会触发离线宽限。若服务端明确给出结论(通过或拒绝),一律直接采纳,
     * 绝不因为"被拒绝"而去读缓存,否则授权吊销将失效。</p>
     *
     * @param plugin     调用方插件实例(用于定位缓存目录)
     * @param pluginName 插件名
     * @param code       验证码(可为空字符串)
     * @return 验证结果
     */
    private static VerifyResult doVerify(JavaPlugin plugin, String pluginName, String code) {
        String url = getServerUrl();
        String machineCode = getMachineCode();
        String nonce = generateNonce();
        String body = buildJsonBody(pluginName, code, machineCode, nonce);

        Exception lastException = null;
        String lastTransientMessage = null;

        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                VerifyResult result = sendRequestAndVerify(
                        plugin, url, body, pluginName, code, machineCode, nonce);

                // 服务端给出了明确结论(通过 / 拒绝 / 假服务器)→ 直接采纳
                if (!result.transientFailure) {
                    return result;
                }

                // 服务端暂时不可用 → 记录后继续重试
                lastTransientMessage = result.message;
                LogUtil.warning("[FlameForge-Verify] 第 " + (attempt + 1)
                        + " 次请求失败(服务端暂时不可用): " + result.message);
            } catch (Exception e) {
                lastException = e;
                LogUtil.warning("[FlameForge-Verify] 第 " + (attempt + 1) + " 次请求失败: "
                        + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        // ---- 走到这里说明:网络异常 或 服务端持续不可用 ----
        String errMsg = "无法连接验证服务器";
        if (lastException != null) {
            errMsg += " (" + lastException.getClass().getSimpleName() + ": "
                    + lastException.getMessage() + ")";
        } else if (lastTransientMessage != null) {
            errMsg += " (" + lastTransientMessage + ")";
        }

        // 尝试离线宽限
        VerifyResult grace = tryOfflineGrace(plugin, pluginName, code, machineCode);
        if (grace != null) {
            return grace;
        }

        return new VerifyResult(false, errMsg + ";且本地无可用的离线授权缓存");
    }

    /**
     * 发送 HTTP 请求并完整校验响应(状态码 + 签名 + nonce + 时间戳)
     *
     * <p>验证通过且签名有效时写入离线缓存;服务端明确吊销授权时删除离线缓存。</p>
     *
     * @param plugin      调用方插件实例(用于定位缓存目录)
     * @param urlStr      目标 URL
     * @param jsonBody    请求体
     * @param pluginName  插件名(用于签名内容重建)
     * @param code        验证码(用于签名内容重建,可为空字符串)
     * @param machineCode 机器码(用于签名内容重建)
     * @param expectNonce 期望回传的 nonce
     * @return 验证结果
     * @throws Exception 网络或解析异常
     */
    private static VerifyResult sendRequestAndVerify(JavaPlugin plugin, String urlStr, String jsonBody,
                                                     String pluginName, String code,
                                                     String machineCode, String expectNonce) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "FlameForge-Verify-Client/1.0");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);

            byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int httpCode = conn.getResponseCode();
            String responseText = readResponse(conn, httpCode);

            // 429 / 5xx 视为"服务端暂时不可用",标记为可宽限的瞬时失败
            if (httpCode == 429) {
                return new VerifyResult(false, "请求过于频繁,稍后重试(HTTP 429)", true, false);
            }
            if (httpCode >= 500) {
                String msg5xx = parseMessage(responseText);
                return new VerifyResult(false,
                        "服务端内部错误 HTTP " + httpCode + (msg5xx != null ? ": " + msg5xx : ""),
                        true, false);
            }
            if (httpCode < 200 || httpCode >= 300) {
                String msg = parseMessage(responseText);
                return new VerifyResult(false,
                        "服务器返回 HTTP " + httpCode + (msg != null ? ": " + msg : ""));
            }

            Boolean isValid = parseIsValid(responseText);
            if (isValid == null) {
                return new VerifyResult(false, "响应格式异常");
            }

            // 若公钥未配置,跳过签名验证(开发模式);此模式下不写缓存,因为没有可校验的签名
            if (isPublicKeyNotConfigured()) {
                return new VerifyResult(isValid, parseMessage(responseText));
            }

            // 以下为严格签名验证路径
            String respNonce = parseField(responseText, NONCE_PATTERN);
            String respSignature = parseField(responseText, SIGNATURE_PATTERN);
            String respTimestampStr = parseField(responseText, TIMESTAMP_PATTERN);

            if (respNonce == null || respSignature == null || respTimestampStr == null) {
                return new VerifyResult(false, "响应缺少签名字段,疑似假服务器");
            }

            // 1. 校验 nonce 回传是否一致(防录制重放)
            if (!expectNonce.equals(respNonce)) {
                return new VerifyResult(false, "响应 nonce 不匹配,疑似重放攻击");
            }

            // 2. 校验 timestamp 是否在允许范围内
            long respTimestamp;
            try {
                respTimestamp = Long.parseLong(respTimestampStr);
            } catch (NumberFormatException e) {
                return new VerifyResult(false, "响应 timestamp 格式错误");
            }
            long nowEpoch = Instant.now().getEpochSecond();
            if (Math.abs(nowEpoch - respTimestamp) > TIMESTAMP_TOLERANCE_SECONDS) {
                return new VerifyResult(false,
                        "响应时间戳漂移过大 (" + (nowEpoch - respTimestamp) + "秒),疑似旧响应重放或时钟不同步");
            }

            // 3. 校验 RSA 签名(客户端重建待签内容,与服务端保持字段顺序一致)
            String signContent = pluginName + "|" + code + "|" + machineCode
                    + "|" + respNonce + "|" + isValid + "|" + respTimestamp;
            if (!verifySignature(signContent, respSignature)) {
                return new VerifyResult(false, "签名验证失败,疑似假服务器");
            }

            // 4. 签名有效 → 维护离线缓存
            if (isValid) {
                // 通过:落盘签名令牌,供服务端故障时使用
                writeCache(plugin, pluginName, signContent, respSignature);
            } else {
                // 明确吊销:立刻清除本地缓存,避免靠宽限期继续使用
                deleteCache(plugin, pluginName);
            }

            return new VerifyResult(isValid, parseMessage(responseText));

        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 用预置公钥验证 SHA256withRSA 签名
     *
     * @param data             待签内容
     * @param signatureBase64  Base64 签名
     * @return 签名是否有效
     */
    private static boolean verifySignature(String data, String signatureBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(PUBLIC_KEY_BASE64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pubKey = kf.generatePublic(keySpec);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pubKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            LogUtil.error("[FlameForge-Verify] 签名验证异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 判断公钥是否未配置(仍为占位符)
     *
     * @return true 表示未配置
     */
    private static boolean isPublicKeyNotConfigured() {
        return PUBLIC_KEY_PLACEHOLDER.equals(PUBLIC_KEY_BASE64);
    }

    /**
     * 生成一次性 nonce(16 字节随机数,Base64URL 编码)
     *
     * @return nonce 字符串
     */
    private static String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ========================================================================
    // 离线宽限期:缓存读写与校验
    // ========================================================================

    /**
     * 尝试用本地离线缓存放行(宽限期机制)
     *
     * <p>校验链条(任一环失败即返回 null,由调用方按原逻辑禁用插件):</p>
     * <ol>
     *   <li>公钥已配置(否则无法校验缓存真伪)</li>
     *   <li>缓存文件存在、格式版本匹配、字段完整</li>
     *   <li>RSA 签名有效(防篡改:改任意一个字节签名即失效)</li>
     *   <li>{@code pluginName|code|machineCode} 前缀与当前环境完全一致
     *       (防跨机器复制缓存、防换绑后继续用旧令牌)</li>
     *   <li>缓存内 isValid 为 true</li>
     *   <li>签名时间戳距今在 {@link #GRACE_PERIOD_SECONDS} 之内,且不超前于当前时间</li>
     * </ol>
     *
     * @param plugin      调用方插件实例
     * @param pluginName  插件名
     * @param code        当前配置里的验证码(可为空字符串)
     * @param machineCode 当前机器码
     * @return 宽限通过时返回成功结果;不可用时返回 null
     */
    private static VerifyResult tryOfflineGrace(JavaPlugin plugin, String pluginName,
                                                String code, String machineCode) {
        // 开发模式(公钥未配置)下无法校验缓存签名,直接不启用宽限
        if (isPublicKeyNotConfigured()) {
            LogUtil.warning("[FlameForge-Verify] 公钥未配置,离线宽限不可用");
            return null;
        }

        File cacheFile = getCacheFile(plugin, pluginName);
        if (cacheFile == null || !cacheFile.isFile()) {
            LogUtil.warning("[FlameForge-Verify] 未找到离线授权缓存(该插件此前从未成功联网验证过)");
            return null;
        }

        String cachedContent = null;
        String cachedSignature = null;
        String cachedVersion = null;

        try {
            List<String> lines = Files.readAllLines(cacheFile.toPath(), StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                if (rawLine == null) {
                    continue;
                }
                String line = rawLine.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if ("v".equals(key)) {
                    cachedVersion = val;
                } else if ("c".equals(key)) {
                    cachedContent = val;
                } else if ("s".equals(key)) {
                    cachedSignature = val;
                }
            }
        } catch (Exception e) {
            LogUtil.warning("[FlameForge-Verify] 读取离线授权缓存失败: " + e.getMessage());
            return null;
        }

        if (!CACHE_VERSION.equals(cachedVersion)) {
            LogUtil.warning("[FlameForge-Verify] 离线授权缓存版本不匹配,已忽略");
            return null;
        }
        if (cachedContent == null || cachedContent.isEmpty()
                || cachedSignature == null || cachedSignature.isEmpty()) {
            LogUtil.warning("[FlameForge-Verify] 离线授权缓存字段缺失,已忽略");
            return null;
        }

        // Base64 解码待签内容
        String signContent;
        try {
            signContent = new String(Base64.getDecoder().decode(cachedContent), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LogUtil.warning("[FlameForge-Verify] 离线授权缓存内容解码失败,已忽略");
            return null;
        }

        // 1. 签名校验(最关键:缓存无法被服主伪造或篡改)
        if (!verifySignature(signContent, cachedSignature)) {
            LogUtil.error("[FlameForge-Verify] 离线授权缓存签名无效(文件被篡改或伪造),已拒绝");
            return null;
        }

        // 2. 前缀比对:pluginName|code|machineCode 必须与当前环境一致
        String expectPrefix = pluginName + "|" + code + "|" + machineCode + "|";
        if (!signContent.startsWith(expectPrefix)) {
            LogUtil.error("[FlameForge-Verify] 离线授权缓存与当前环境不匹配"
                    + "(插件名/验证码/机器码已变化,或缓存来自其他机器),已拒绝");
            return null;
        }

        // 3. 解析剩余字段:nonce|isValid|timestamp
        String remainder = signContent.substring(expectPrefix.length());
        String[] parts = remainder.split("\\|", -1);
        if (parts.length != 3) {
            LogUtil.warning("[FlameForge-Verify] 离线授权缓存结构异常,已忽略");
            return null;
        }
        String cachedIsValid = parts[1];
        long cachedTimestamp;
        try {
            cachedTimestamp = Long.parseLong(parts[2].trim());
        } catch (NumberFormatException e) {
            LogUtil.warning("[FlameForge-Verify] 离线授权缓存时间戳格式错误,已忽略");
            return null;
        }

        // 4. 缓存必须是"验证通过"的令牌
        if (!"true".equalsIgnoreCase(cachedIsValid)) {
            LogUtil.error("[FlameForge-Verify] 离线授权缓存记录的是未通过状态,已拒绝");
            return null;
        }

        // 5. 时间窗口校验
        long now = Instant.now().getEpochSecond();
        long age = now - cachedTimestamp;
        if (age < -TIMESTAMP_TOLERANCE_SECONDS) {
            LogUtil.error("[FlameForge-Verify] 离线授权缓存时间戳超前于当前时间 "
                    + (-age) + " 秒(系统时钟异常),已拒绝");
            return null;
        }
        if (age > GRACE_PERIOD_SECONDS) {
            LogUtil.error("[FlameForge-Verify] 离线授权缓存已超过 "
                    + (GRACE_PERIOD_SECONDS / 86400L) + " 天宽限期(已过期 "
                    + (age / 86400L) + " 天),必须联网重新验证");
            return null;
        }

        // ---- 宽限通过 ----
        long remain = GRACE_PERIOD_SECONDS - age;
        long remainDays = remain / 86400L;
        long remainHours = (remain % 86400L) / 3600L;

        LogUtil.warning("");
        LogUtil.warning("§e╔══════════════════════════════════════════════════════╗");
        LogUtil.warning("§e║   [FlameForge] 验证服务器不可达,已启用离线宽限模式   ║");
        LogUtil.warning("§e╚══════════════════════════════════════════════════════╝");
        LogUtil.warning("§e  插件: " + pluginName);
        LogUtil.warning("§e  上次成功验证: " + Instant.ofEpochSecond(cachedTimestamp));
        LogUtil.warning("§e  剩余宽限: " + remainDays + " 天 " + remainHours + " 小时");
        LogUtil.warning("§e  超期后插件将无法启用,请尽快恢复与验证服务器的网络连通");
        LogUtil.warning("");

        return new VerifyResult(true,
                "离线宽限模式(剩余 " + remainDays + " 天 " + remainHours + " 小时)",
                false, true);
    }

    /**
     * 写入离线授权缓存
     *
     * <p>失败只记 WARN,不影响本次验证结果(缓存是可选增强,不是必需品)。</p>
     *
     * @param plugin          调用方插件实例
     * @param pluginName      插件名
     * @param signContent     服务端签名过的原始待签内容
     * @param signatureBase64 服务端返回的 Base64 签名
     */
    private static void writeCache(JavaPlugin plugin, String pluginName,
                                   String signContent, String signatureBase64) {
        File cacheFile = getCacheFile(plugin, pluginName);
        if (cacheFile == null) {
            return;
        }
        try {
            String encodedContent = Base64.getEncoder()
                    .encodeToString(signContent.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(512);
            sb.append("# FlameForge-Verify 离线授权缓存 - 请勿手动编辑\n");
            sb.append("# 内容由验证服务器私钥签名,任何修改都会导致签名校验失败\n");
            sb.append("# 生成时间: ").append(Instant.now()).append('\n');
            sb.append("v=").append(CACHE_VERSION).append('\n');
            sb.append("c=").append(encodedContent).append('\n');
            sb.append("s=").append(signatureBase64).append('\n');

            Files.write(cacheFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            LogUtil.debug("[FlameForge-Verify] 离线授权缓存已更新: " + cacheFile.getAbsolutePath());
        } catch (Exception e) {
            LogUtil.warning("[FlameForge-Verify] 写入离线授权缓存失败(不影响本次验证): "
                    + e.getMessage());
        }
    }

    /**
     * 删除离线授权缓存(授权被服务端明确吊销时调用)
     *
     * @param plugin     调用方插件实例
     * @param pluginName 插件名
     */
    private static void deleteCache(JavaPlugin plugin, String pluginName) {
        File cacheFile = getCacheFile(plugin, pluginName);
        if (cacheFile == null || !cacheFile.isFile()) {
            return;
        }
        try {
            if (Files.deleteIfExists(cacheFile.toPath())) {
                LogUtil.warning("[FlameForge-Verify] 授权已被服务端拒绝,离线授权缓存已清除");
            }
        } catch (Exception e) {
            LogUtil.warning("[FlameForge-Verify] 清除离线授权缓存失败: " + e.getMessage());
        }
    }

    /**
     * 定位离线缓存文件
     *
     * <p>优先放在 YukiNoaAPI 的数据目录下集中管理,
     * 若 YukiNoaAPI 主实例不可用则退回调用方插件的数据目录。</p>
     *
     * @param plugin     调用方插件实例
     * @param pluginName 插件名(作为文件名,会做字符清洗)
     * @return 缓存文件对象;无法确定目录时返回 null
     */
    private static File getCacheFile(JavaPlugin plugin, String pluginName) {
        try {
            File base = null;
            if (Main.plugin != null && Main.plugin.getDataFolder() != null) {
                base = Main.plugin.getDataFolder();
            } else if (plugin != null && plugin.getDataFolder() != null) {
                base = plugin.getDataFolder();
            }
            if (base == null) {
                return null;
            }

            File dir = new File(base, CACHE_DIR_NAME);
            if (!dir.isDirectory() && !dir.mkdirs()) {
                LogUtil.warning("[FlameForge-Verify] 无法创建离线缓存目录: " + dir.getAbsolutePath());
                return null;
            }
            return new File(dir, sanitizeFileName(pluginName) + CACHE_FILE_SUFFIX);
        } catch (Exception e) {
            LogUtil.warning("[FlameForge-Verify] 定位离线缓存文件失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 清洗字符串用作文件名(只保留字母数字与 . _ -,防路径穿越)
     *
     * @param name 原始名称
     * @return 安全文件名(空时返回 "unnamed")
     */
    private static String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed";
        }
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length() && sb.length() < CACHE_FILE_NAME_MAX_LEN; i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String result = sb.toString();
        return result.isEmpty() ? "unnamed" : result;
    }

    // ========================================================================
    // HTTP 响应读取与解析
    // ========================================================================

    /**
     * 读取 HTTP 响应体
     *
     * @param conn     连接
     * @param httpCode 状态码
     * @return 响应文本
     */
    private static String readResponse(HttpURLConnection conn, int httpCode) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                httpCode >= 400 && conn.getErrorStream() != null
                        ? conn.getErrorStream()
                        : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    /**
     * 解析 isValid / success 字段
     *
     * @param response 响应文本
     * @return 布尔值或 null
     */
    private static Boolean parseIsValid(String response) {
        if (response == null || response.isEmpty()) return null;
        Matcher m = ISVALID_PATTERN.matcher(response);
        if (m.find()) {
            return "true".equalsIgnoreCase(m.group(1));
        }
        return null;
    }

    /**
     * 解析 message 字段(带 JSON 转义处理)
     *
     * @param response 响应文本
     * @return message 或 null
     */
    private static String parseMessage(String response) {
        if (response == null || response.isEmpty()) return null;
        Matcher m = MESSAGE_PATTERN.matcher(response);
        if (m.find()) {
            return m.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t");
        }
        return null;
    }

    /**
     * 通用字段正则提取
     *
     * @param response 响应文本
     * @param pattern  匹配模式(必须有一个捕获组)
     * @return 捕获组内容或 null
     */
    private static String parseField(String response, Pattern pattern) {
        if (response == null || response.isEmpty()) return null;
        Matcher m = pattern.matcher(response);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 构造请求 JSON 体
     *
     * @param pluginName  插件名
     * @param code        验证码(可为空字符串)
     * @param machineCode 机器码
     * @param nonce       一次性随机数
     * @return JSON 字符串
     */
    private static String buildJsonBody(String pluginName, String code, String machineCode, String nonce) {
        return "{"
                + "\"pluginName\":\"" + jsonEscape(pluginName) + "\","
                + "\"validationCode\":\"" + jsonEscape(code) + "\","
                + "\"machineCode\":\"" + jsonEscape(machineCode) + "\","
                + "\"nonce\":\"" + jsonEscape(nonce) + "\""
                + "}";
    }

    /**
     * JSON 字符串转义
     *
     * @param s 原文
     * @return 转义后字符串
     */
    private static String jsonEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // ========================================================================
    // 服务端 URL 读取
    // ========================================================================

    /**
     * 从 YukiNoaAPI 的 config.yml 读取自定义服务端 URL
     *
     * @return 服务端 URL
     */
    private static String getServerUrl() {
        try {
            if (Main.plugin != null && Main.config != null) {
                String custom = Main.config.getString("Verify.ServerUrl", "");
                if (custom != null && !custom.trim().isEmpty()) {
                    return custom.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_URL;
    }

    // ========================================================================
    // 机器码多因子指纹采集
    // ========================================================================

    /**
     * 构建机器码
     *
     * @return 机器码字符串
     */
    private static String buildMachineCode() {
        List<String> factors = new ArrayList<>();
        collectMacAddresses(factors);
        collectHostName(factors);
        collectUserHome(factors);
        collectOsInfo(factors);
        collectCpuCount(factors);
        collectUserName(factors);

        if (factors.isEmpty()) {
            String fallback = "UNKNOWN-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            LogUtil.warning("[FlameForge-Verify] 所有机器码因子采集失败,使用兜底随机值: " + fallback);
            return fallback;
        }

        String osPrefix = getOsPrefix();
        String hostPrefix = getHostPrefix();

        Collections.sort(factors);
        String hash = sha256Hex(String.join("||", factors));
        String shortHash = hash.length() >= 16 ? hash.substring(0, 16) : hash;

        StringBuilder result = new StringBuilder(64);
        if (osPrefix != null && !osPrefix.isEmpty()) {
            result.append(osPrefix).append('-');
        }
        if (hostPrefix != null && !hostPrefix.isEmpty()) {
            result.append(hostPrefix).append('-');
        }
        result.append(shortHash);

        String finalCode = result.toString();
        if (finalCode.length() > MACHINE_CODE_MAX_LEN) {
            finalCode = finalCode.substring(0, MACHINE_CODE_MAX_LEN);
        }

        LogUtil.debug("[FlameForge-Verify] 机器码生成成功 (因子数=" + factors.size()
                + ", 结果=" + finalCode + ")");
        return finalCode;
    }

    /**
     * 收集所有物理网卡 MAC 地址
     *
     * @param factors 因子列表
     */
    private static void collectMacAddresses(List<String> factors) {
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            List<String> macs = new ArrayList<>();
            while (nics != null && nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                try {
                    if (nic.isLoopback() || nic.isVirtual()) continue;
                    byte[] mac = nic.getHardwareAddress();
                    if (mac == null || mac.length == 0) continue;
                    StringBuilder sb = new StringBuilder(mac.length * 2);
                    for (byte b : mac) {
                        sb.append(String.format("%02x", b));
                    }
                    macs.add(sb.toString());
                } catch (Exception ignored) {
                }
            }
            if (!macs.isEmpty()) {
                Collections.sort(macs);
                factors.add("MAC=" + String.join(",", macs));
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 收集主机名
     *
     * @param factors 因子列表
     */
    private static void collectHostName(List<String> factors) {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            if (host != null && !host.isEmpty()) {
                factors.add("HOST=" + host);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 收集用户主目录
     *
     * @param factors 因子列表
     */
    private static void collectUserHome(List<String> factors) {
        try {
            String userHome = System.getProperty("user.home");
            if (userHome != null && !userHome.isEmpty()) {
                factors.add("HOME=" + userHome);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 收集 OS 信息
     *
     * @param factors 因子列表
     */
    private static void collectOsInfo(List<String> factors) {
        try {
            String os = safeStrProp("os.name") + "|"
                    + safeStrProp("os.arch") + "|"
                    + safeStrProp("os.version");
            factors.add("OS=" + os);
        } catch (Exception ignored) {
        }
    }

    /**
     * 收集 CPU 核心数
     *
     * @param factors 因子列表
     */
    private static void collectCpuCount(List<String> factors) {
        try {
            factors.add("CPU=" + Runtime.getRuntime().availableProcessors());
        } catch (Exception ignored) {
        }
    }

    /**
     * 收集系统账户名
     *
     * @param factors 因子列表
     */
    private static void collectUserName(List<String> factors) {
        try {
            String user = System.getProperty("user.name");
            if (user != null && !user.isEmpty()) {
                factors.add("USER=" + user);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 安全读取系统属性
     *
     * @param key 属性键
     * @return 值或 "UNKNOWN"
     */
    private static String safeStrProp(String key) {
        try {
            String v = System.getProperty(key);
            return (v == null || v.isEmpty()) ? "UNKNOWN" : v;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * 根据 os.name 生成可读前缀
     *
     * @return linux/windows/macos/bsd/unknown
     */
    private static String getOsPrefix() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("linux"))   return "linux";
            if (os.contains("windows")) return "windows";
            if (os.contains("mac"))     return "macos";
            if (os.contains("bsd"))     return "bsd";
            if (os.contains("sunos") || os.contains("solaris")) return "solaris";
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 生成清洗后的主机名前缀
     *
     * @return 主机名前缀(最长 16 字符)
     */
    private static String getHostPrefix() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            if (host == null || host.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < host.length() && sb.length() < 16; i++) {
                char c = host.charAt(i);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9') || c == '-') {
                    sb.append(Character.toLowerCase(c));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * SHA-256 哈希 hex 输出
     *
     * @param input 输入
     * @return hex 字符串
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    // ========================================================================
    // 失败横幅 + 禁用插件
    // ========================================================================

    /**
     * 打印告警横幅并调度下一 tick 禁用插件
     *
     * @param plugin 插件实例
     * @param reason 失败原因
     */
    private static void bannerAndDisable(final JavaPlugin plugin, String reason) {
        LogUtil.error("");
        LogUtil.error("§c╔══════════════════════════════════════════════════════╗");
        LogUtil.error("§c║         [FlameForge] 插件授权验证失败                ║");
        LogUtil.error("§c╚══════════════════════════════════════════════════════╝");
        LogUtil.error("§c  插件: " + plugin.getName());
        LogUtil.error("§c  原因: " + reason);
        LogUtil.error("§c  如需获取验证码,请联系@天御炎科技: https://www.flameforge.tech/");
        LogUtil.error("§c  如需更换绑定,请访问: https://verify.flameforge.tech/portal");
        LogUtil.error("");

        try {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (plugin.isEnabled()) {
                            Bukkit.getPluginManager().disablePlugin(plugin);
                        }
                    } catch (Exception e) {
                        LogUtil.error("[FlameForge-Verify] 禁用插件时发生异常: " + e.getMessage());
                    }
                }
            }.runTask(Main.plugin != null ? Main.plugin : plugin);
        } catch (Exception e) {
            try {
                Bukkit.getPluginManager().disablePlugin(plugin);
            } catch (Exception ignored) {
            }
        }
    }

    // ========================================================================
    // 内部数据类
    // ========================================================================

    /** 验证结果 */
    private static final class VerifyResult {
        /** 是否验证通过 */
        final boolean success;
        /** 服务端返回的 message(可能为 null) */
        final String message;
        /**
         * 是否为"瞬时失败"(网络/服务端故障,可重试并可回退到离线宽限)
         *
         * <p>为 false 表示服务端已给出明确结论(通过或拒绝),不得回退到缓存。</p>
         */
        final boolean transientFailure;
        /** 是否通过离线宽限期放行(仅用于日志区分) */
        final boolean offlineGrace;

        /**
         * 构造(明确结论,非瞬时失败,非离线宽限)
         *
         * @param success 是否通过
         * @param message 消息
         */
        VerifyResult(boolean success, String message) {
            this(success, message, false, false);
        }

        /**
         * 构造(完整参数)
         *
         * @param success          是否通过
         * @param message          消息
         * @param transientFailure 是否为可宽限的瞬时失败
         * @param offlineGrace     是否通过离线宽限放行
         */
        VerifyResult(boolean success, String message, boolean transientFailure, boolean offlineGrace) {
            this.success = success;
            this.message = message;
            this.transientFailure = transientFailure;
            this.offlineGrace = offlineGrace;
        }
    }
}