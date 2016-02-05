package com.emc.gradle.protobuf

/**
 * Configuration for the protobuf plugin.
 * Java source is always generated, but CPP and python can be disabled by calling {@link #disableCpp()} and 
 * {@link #disablePython()} respectively
 */
class ProtobufExtension {
    /** Version of protocol compiler required (defaults to 2.5.0). */
    String version = "2.5.0"
    /** Path to the protocol compiler executable. */
    String executable
    /** Proto source directory. */
    String src = "src/main/proto"
    /** Output directory for java files. */
    String javaDir = "src/generated/java"
    /** Output directory for cpp files. */
    String cppDir = "src/generated/cpp"
    /** Output directory for python files. */
    String pythonDir = "src/generated/python"
    
    void disableCpp() {
        cppDir = null
    }
    void disablePython() {
        pythonDir = null
    }
}