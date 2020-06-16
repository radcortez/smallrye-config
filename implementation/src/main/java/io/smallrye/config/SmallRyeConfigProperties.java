package io.smallrye.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.microprofile.config.Config;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class SmallRyeConfigProperties {
    public static <T> T mapConfigProperties(final Class<T> klass, final String prefix, final Config config) {
        final String prefixLookup = prefix != null && !prefix.isEmpty() ? prefix + "." : "";

        if (!klass.isInterface() && !Modifier.isAbstract(klass.getModifiers())) {
            return mapConfigPropertiesClass(klass, prefixLookup, config);
        } else if (klass.isInterface()) {
            return mapConfigPropertiesInterface(klass, prefixLookup, config);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static <T> T mapConfigPropertiesClass(final Class<T> klass, final String prefix, final Config config) {
        try {
            final T instance = klass.getDeclaredConstructor().newInstance();
            final Field[] declaredFields = klass.getDeclaredFields();

            for (final Field declaredField : declaredFields) {
                declaredField.set(instance, config.getValue(prefix + declaredField.getName(), declaredField.getType()));
            }

            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private static <T> T mapConfigPropertiesInterface(final Class<T> klass, final String prefix, final Config config) {
        try {
            final String generatedClass = klass.getName() + "_ConfigProperties";
            final ClassCreator classCreator = ClassCreator.builder().classOutput((name, data) -> {
                try {
                    final Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class,
                            byte[].class, int.class, int.class);
                    defineClass.setAccessible(true);
                    defineClass.invoke(klass.getClassLoader(), generatedClass, data, 0, data.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).className(generatedClass).interfaces(klass).build();

            // Declare field
            final FieldDescriptor configField = FieldDescriptor.of(generatedClass, "config", SmallRyeConfig.class);
            classCreator.getFieldCreator(configField);

            // Add constructor with SmallRyeConfig
            final MethodCreator constructor = classCreator.getMethodCreator(
                    MethodDescriptor.ofConstructor(generatedClass, SmallRyeConfig.class.getName()));
            constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), constructor.getThis());
            constructor.writeInstanceField(configField, constructor.getThis(), constructor.getMethodParam(0));
            constructor.returnValue(null);

            // Provide implementations for all methods, by doing a lookup to SmallRyeConfig property.
            for (final Method declaredMethod : klass.getDeclaredMethods()) {
                final String configName = declaredMethod.getName().startsWith("get")
                        ? declaredMethod.getName().substring(3, 4).toLowerCase() +
                                declaredMethod.getName().substring(4)
                        : declaredMethod.getName();

                final MethodDescriptor methodDescriptor = MethodDescriptor.ofMethod(declaredMethod);
                final MethodCreator methodCreator = classCreator.getMethodCreator(methodDescriptor);

                final MethodDescriptor configGetValue = MethodDescriptor.ofMethod(SmallRyeConfig.class, "getValue",
                        Object.class, String.class, Class.class);
                final ResultHandle getValue = methodCreator.invokeVirtualMethod(
                        configGetValue,
                        methodCreator.readInstanceField(configField, methodCreator.getThis()),
                        methodCreator.load(prefix + configName),
                        methodCreator.loadClass(declaredMethod.getReturnType()));

                methodCreator.returnValue(getValue);
            }

            classCreator.close();

            final Class<?> implementation = klass.getClassLoader().loadClass(klass.getName() + "_ConfigProperties");
            return (T) implementation.getDeclaredConstructor(SmallRyeConfig.class).newInstance(config);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
