package com.github.novotnyr.idea.jwt;

import java.util.HashSet;
import java.util.Set;

public class SignatureAlgorithm {
    public static final String RS256 = "RS256";
    public static final String HS256 = "HS256";
    public static final String HS384 = "HS384";

    public static final Set<String> SUPPORTED_ALGORITHMS = new HashSet<String>() {
        {
            add(RS256);
            add(HS256);
            add(HS384);
        }
    };

    public static boolean isSupported(String algorithm) {
        return SUPPORTED_ALGORITHMS.contains(algorithm);
    }

}
