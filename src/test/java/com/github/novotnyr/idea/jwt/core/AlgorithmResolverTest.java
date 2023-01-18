package com.github.novotnyr.idea.jwt.core;

import com.github.novotnyr.idea.jwt.validation.UnknownAlgorithmException;
import org.junit.Test;

public class AlgorithmResolverTest {
    @Test(expected = UnknownAlgorithmException.class)
    public void testUnsupportedAlgorithm() {
        AlgorithmResolver algorithmResolver = new AlgorithmResolver();
        algorithmResolver.resolve("ABC123_IS_UNSUPPORTED", null);
    }
}