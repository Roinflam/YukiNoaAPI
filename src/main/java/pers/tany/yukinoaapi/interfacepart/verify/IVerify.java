package pers.tany.yukinoaapi.interfacepart.verify;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.LogUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
 * 插件授权验证接口(含挑战-响应签名机制)
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
 * <h3>公钥配置</h3>
 * <p><b>首次部署必做:</b>运行一次后端(签名服务会自动生成密钥对),
 * 在后端日志或 {@code public-key.txt} 文件里复制公钥,
 * 替换下方 {@link #PUBLIC_KEY_BASE64} 常量,然后重新编译 YukiNoaAPI。</p>
 *
 * <p>若未配置(保持占位符),签名验证会被跳过(开发模式),
 * 控制台输出明显的警告日志,便于你在本地调试。</p>
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

        VerifyResult result = doVerify(pluginName.trim(), trimmedCode);
        if (result.success) {
            LogUtil.success("[" + plugin.getName() + "] 授权验证通过 (plugin=" + pluginName + ")");
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
    // 验证请求实现(含挑战-响应签名验证)
    // ========================================================================

    /**
     * 执行 HTTP 验证请求,含重试
     *
     * @param pluginName 插件名
     * @param code       验证码(可为空字符串)
     * @return 验证结果
     */
    private static VerifyResult doVerify(String pluginName, String code) {
        String url = getServerUrl();
        String machineCode = getMachineCode();
        String nonce = generateNonce();
        String body = buildJsonBody(pluginName, code, machineCode, nonce);

        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRY; attempt++) {
            try {
                return sendRequestAndVerify(url, body, pluginName, code, machineCode, nonce);
            } catch (Exception e) {
                lastException = e;
                LogUtil.warning("[FlameForge-Verify] 第 " + (attempt + 1) + " 次请求失败: "
                        + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        String errMsg = "无法连接验证服务器";
        if (lastException != null) {
            errMsg += " (" + lastException.getClass().getSimpleName() + ": "
                    + lastException.getMessage() + ")";
        }
        return new VerifyResult(false, errMsg);
    }

    /**
     * 发送 HTTP 请求并完整校验响应(状态码 + 签名 + nonce + 时间戳)
     *
     * @param urlStr      目标 URL
     * @param jsonBody    请求体
     * @param pluginName  插件名(用于签名内容重建)
     * @param code        验证码(用于签名内容重建,可为空字符串)
     * @param machineCode 机器码(用于签名内容重建)
     * @param expectNonce 期望回传的 nonce
     * @return 验证结果
     * @throws Exception 网络或解析异常
     */
    private static VerifyResult sendRequestAndVerify(String urlStr, String jsonBody,
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

            if (httpCode == 429) {
                return new VerifyResult(false, "请求过于频繁,稍后重试(HTTP 429)");
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

            // 若公钥未配置,跳过签名验证(开发模式)
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
         * 构造
         *
         * @param success 是否通过
         * @param message 消息
         */
        VerifyResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}