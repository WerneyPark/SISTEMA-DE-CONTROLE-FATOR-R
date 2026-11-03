package com.fatorr.backend.util;

import com.fatorr.backend.domain.TipoPerfil;
import com.fatorr.backend.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitário nativo de JWT baseado em HMAC-SHA256 para geração, validação e extração de claims.
 * Implementação puramente nativa em Java Standard, sem dependências externas.
 */
@Component
public class JwtUtils {

    public static final String DEFAULT_SECRET = "fator-r-super-secret-jwt-key-for-hmac-sha256-signature-at-least-256-bits";
    public static final long DEFAULT_EXPIRATION_MS = 86_400_000L; // 24 horas

    private static final Pattern JSON_KEY_VALUE_PATTERN = Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+)|(true|false)|null)"
    );

    private final String secret;
    private final long expirationMs;

    public JwtUtils() {
        this(DEFAULT_SECRET, DEFAULT_EXPIRATION_MS);
    }

    public JwtUtils(
            @Value("${jwt.secret:" + DEFAULT_SECRET + "}") String secret,
            @Value("${jwt.expiration:" + DEFAULT_EXPIRATION_MS + "}") long expirationMs) {
        this.secret = (secret != null && !secret.isBlank()) ? secret : DEFAULT_SECRET;
        this.expirationMs = expirationMs > 0 ? expirationMs : DEFAULT_EXPIRATION_MS;
    }

    /**
     * Gera token JWT com as claims obrigatórias: cpf, userId, uuidId, tipoPerfil.
     */
    public String generateToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", usuario.getId());
        claims.put("uuidId", usuario.getUuidId());
        claims.put("cpf", usuario.getCpf());

        String tipoPerfil = null;
        if (usuario.getTipoPerfil() != null) {
            tipoPerfil = usuario.getTipoPerfil().name();
        } else if (usuario.getTipo() != null) {
            tipoPerfil = usuario.getTipo().name().contains("ADMIN") ? TipoPerfil.CONTABILISTA.name() : TipoPerfil.USUARIO.name();
        }
        claims.put("tipoPerfil", tipoPerfil);
        claims.put("sub", usuario.getCpf() != null ? usuario.getCpf() : usuario.getEmail());

        long nowSeconds = Instant.now().getEpochSecond();
        long expSeconds = nowSeconds + (expirationMs / 1000);
        claims.put("iat", nowSeconds);
        claims.put("exp", expSeconds);

        return createToken(claims);
    }

    /**
     * Cria e assina o token no formato header.payload.signature
     */
    public String createToken(Map<String, Object> claims) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            String headerJson = toJson(header);
            String payloadJson = toJson(claims);

            String headerEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String content = headerEncoded + "." + payloadEncoded;
            String signature = sign(content, this.secret);

            return content + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    /**
     * Valida integridade e tempo de expiração do token JWT.
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return false;
            }
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
            String[] parts = cleanToken.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content, this.secret);

            // Comparação em tempo constante para evitar timing attacks
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            // Validação de expiração
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            Map<String, Object> claims = parseJson(payloadJson);
            if (claims.containsKey("exp")) {
                Object expObj = claims.get("exp");
                long exp = (expObj instanceof Number n) ? n.longValue() : Long.parseLong(expObj.toString());
                long nowSeconds = Instant.now().getEpochSecond();
                if (nowSeconds >= exp) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrai todas as claims em formato Map.
     */
    public Map<String, Object> extractAllClaims(String token) {
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Token JWT inválido ou expirado");
        }
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        String[] parts = cleanToken.split("\\.");
        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
        return parseJson(payloadJson);
    }

    public String extractCpf(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object cpf = claims.get("cpf");
        return cpf != null ? cpf.toString() : null;
    }

    public Long extractUserId(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        } else if (userId != null) {
            return Long.parseLong(userId.toString());
        }
        return null;
    }

    public String extractUuidId(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object uuidId = claims.get("uuidId");
        return uuidId != null ? uuidId.toString() : null;
    }

    public String extractTipoPerfil(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object tipoPerfil = claims.get("tipoPerfil");
        return tipoPerfil != null ? tipoPerfil.toString() : null;
    }

    private String sign(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() == null) continue;
            if (!first) sb.append(",");
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object val = entry.getValue();
            if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else {
                sb.append("\"").append(escapeJson(val.toString())).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> parseJson(String json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null) return map;
        Matcher m = JSON_KEY_VALUE_PATTERN.matcher(json);
        while (m.find()) {
            String key = m.group(1);
            if (m.group(2) != null) {
                map.put(key, unescapeJson(m.group(2)));
            } else if (m.group(3) != null) {
                map.put(key, Long.parseLong(m.group(3)));
            } else if (m.group(4) != null) {
                map.put(key, Boolean.parseBoolean(m.group(4)));
            }
        }
        return map;
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
