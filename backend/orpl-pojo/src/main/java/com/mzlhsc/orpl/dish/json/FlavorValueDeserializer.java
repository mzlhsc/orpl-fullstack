package com.mzlhsc.orpl.dish.json;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * 口味值反序列化器（Jackson 3）。
 * <p>
 * 前端既可能传字符串 {@code "无糖,少糖"}，也可能传数组 {@code ["无糖","少糖"]}，
 * 数据库里统一按英文逗号拼接的字符串保存。
 * <p>
 * Spring Boot 4 的 MVC 默认使用 Jackson 3（tools.jackson），
 * Jackson 2 的注解不会生效，所以这里显式指定 Jackson 3 的 ValueDeserializer。
 */
public class FlavorValueDeserializer extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.START_ARRAY) {
            StringBuilder builder = new StringBuilder();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(p.getString());
            }
            return builder.toString();
        }
        // 字符串 / 数字 / 布尔等标量值统一转成字符串
        return p.getValueAsString();
    }
}
