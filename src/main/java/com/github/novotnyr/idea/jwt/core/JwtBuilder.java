package com.github.novotnyr.idea.jwt.core;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.impl.ClaimsHolder;
import com.auth0.jwt.impl.PublicClaims;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.novotnyr.idea.jwt.core.ser.PayloadSerializer;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class JwtBuilder {
    private AlgorithmResolver algorithmResolver = new AlgorithmResolver();

    private Map<String, Object> headerClaims = new LinkedHashMap<>();

    private Map<String, Object> payloadClaims = new LinkedHashMap<>();

    private final ObjectMapper mapper;

    public JwtBuilder() {
        this.mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ClaimsHolder.class, new PayloadSerializer());
        this.mapper.registerModule(module);
    }

    public JwtBuilder withClaim(NamedClaim<?> claim) {
        this.payloadClaims.put(claim.getName(), claim.getValue());
        return this;
    }

    public String sign(String algorithmName, Object securityContexts) {
        Algorithm algorithm = this.algorithmResolver.resolve(algorithmName, securityContexts);
        this.headerClaims.put(PublicClaims.ALGORITHM, algorithm.getName());
        this.headerClaims.put(PublicClaims.TYPE, "JWT");
        String signingKeyId = algorithm.getSigningKeyId();
        if (signingKeyId != null) {
            this.headerClaims.put(PublicClaims.KEY_ID, signingKeyId);
        }

        try {
            String headerJson = this.mapper.writeValueAsString(this.headerClaims);
            String payloadJson = this.mapper.writeValueAsString(new ClaimsHolder(this.payloadClaims));

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
