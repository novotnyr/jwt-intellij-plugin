package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.HeaderParams;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.novotnyr.idea.jwt.SignatureContext;
import com.github.novotnyr.idea.jwt.core.ser.PayloadClaimsSerializer;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class JwtBuilder {
    private AlgorithmResolver algorithmResolver = new AlgorithmResolver();

    private Map<String, Object> headerClaims = new LinkedHashMap<>();

    private PayloadClaims payloadClaims = new PayloadClaims();

    private final ObjectMapper mapper;

    public JwtBuilder() {
        this.mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(PayloadClaims.class, new PayloadClaimsSerializer());
        this.mapper.registerModule(module);
    }

    public JwtBuilder withClaim(NamedClaim<?> claim) {
        this.payloadClaims.add(claim);
        return this;
    }

    public String sign(String algorithmName, SignatureContext signatureContext) {
        Algorithm algorithm = this.algorithmResolver.resolve(algorithmName, signatureContext);
        this.headerClaims.put(HeaderParams.ALGORITHM, algorithm.getName());
        this.headerClaims.put(HeaderParams.TYPE, "JWT");
        String signingKeyId = algorithm.getSigningKeyId();
        if (signingKeyId != null) {
            this.headerClaims.put(HeaderParams.KEY_ID, signingKeyId);
        }

        try {
            String headerJson = this.mapper.writeValueAsString(this.headerClaims);
            String payloadJson = this.mapper.writeValueAsString(this.payloadClaims);

            String header = Base64.encodeBase64URLSafeString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payload = Base64.encodeBase64URLSafeString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String content = header + "." + payload;

            byte[] signatureBytes = algorithm.sign(content.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.encodeBase64URLSafeString((signatureBytes));

            return content + "." + signature;
        } catch (JsonProcessingException e) {
            throw new JWTCreationException("Some of the Claims couldn't be converted to a valid JSON format.", e);
        }
    }
}
