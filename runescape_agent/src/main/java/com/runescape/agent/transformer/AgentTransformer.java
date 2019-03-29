/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.runescape.agent.transformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import com.runescape.agent.RuneAgent;
import com.runescape.agent.util.ClassModifier;

/**
 *
 * @author unsignedbyte
 */
public abstract class AgentTransformer implements ClassFileTransformer {

    protected final RuneAgent agent;
    protected final HashMap<String, ClassModifier> modifiers = new HashMap<String, ClassModifier>();

    public AgentTransformer(RuneAgent agent) {
        this.agent = agent;
    }

    @Override
    public abstract byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException;

    public RuneAgent getAgent() {
        return agent;
    }

    public HashMap<String, ClassModifier> getModifiers() {
        return modifiers;
    }
}
