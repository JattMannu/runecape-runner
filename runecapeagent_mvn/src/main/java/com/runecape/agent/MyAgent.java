package com.runecape.agent;



import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;

public class MyAgent {
    public static void premain(String arguments, Instrumentation inst) {
        System.out.println("\n---------------------------------this is an bytebuddy sample ---------------------------------------");

        AgentBuilder agentBuilder = new AgentBuilder.Default();
        AgentBuilder.Transformer transformer = new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                    TypeDescription typeDescription,
                                                    ClassLoader classLoader, JavaModule javaModule) {
                String className = typeDescription.getCanonicalName();
                String packageName = typeDescription.getPackage().getName();
                System.out.println("++++++++ class name = " + className);
                System.out.println("++++++++ package name = " + packageName);


                //ElementMatchers.named("sayHello")
                //ElementMatchers.nameStartsWith("say")
                //ElementMatchers.nameEndsWith("Hello")
                //...
                builder = builder.method(ElementMatchers.any())//匹配任意方法
                        .intercept(MethodDelegation.to(new SimplePackageInstanceMethodInterceptor()));
                return builder;
            }
        };
        //匹配net.beeapm.bytebuddy.hello.sample包的里的类
        agentBuilder = agentBuilder.type(ElementMatchers.nameStartsWith("app")).transform(transformer);

        AgentBuilder.Listener listener = new AgentBuilder.Listener() {

            @Override
            public void onDiscovery(String s, ClassLoader classLoader, JavaModule javaModule, boolean b) {
                //System.out.println("onDiscovery"+s);
            }

            @Override
            public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, boolean b, DynamicType dynamicType) {
            }

            @Override
            public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, boolean b) {

            }

            @Override
            public void onError(String s, ClassLoader classLoader, JavaModule javaModule, boolean b, Throwable throwable) {
                System.out.println("onError"+s);
            }

            @Override
            public void onComplete(String s, ClassLoader classLoader, JavaModule javaModule, boolean b) {
                //System.out.println("onComplete"+s);
            }
        };

        agentBuilder.with(listener).installOn(inst);
    }
}
