package com.github.novotnyr.idea.jwt.datatype;

import java.util.Arrays;
import java.util.List;

public class DataTypeRegistry {
    private static final DataTypeRegistry INSTANCE = new DataTypeRegistry();

    public static DataTypeRegistry getInstance() {
        return INSTANCE;
    }

    public List<DataType> getDataTypes() {
        return Arrays.asList(DataType.values());
    }

    public enum DataType {
        STRING,
        BOOLEAN,
        NUMERIC,
        TIMESTAMP,
        RAW
    }
}
