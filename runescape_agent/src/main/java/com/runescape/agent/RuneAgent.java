/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.runescape.agent;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.runescape.agent.event.RuneAgentEvent;
import com.runescape.agent.event.RuneAgentEventTypes;
import com.runescape.agent.gui.AgentFrame;
import com.runescape.agent.listener.RuneAgentEventListener;
import com.runescape.agent.transformer.AgentTransformer;

/**
 *
 * @author unsignedbyte
 */
public class RuneAgent {
    public static final double VERSION = 1.3d;
    private List<RuneAgentEventListener> listeners;
    private AgentFrame agentFrame;
    private final ScriptEngine engine;
    private final Instrumentation instrumentation;
    private final HashMap<String, Object> modifiedObjects;

    public RuneAgent(Instrumentation i) {
        this.listeners = new ArrayList<RuneAgentEventListener>();
        this.modifiedObjects = new HashMap<String, Object>();
        this.instrumentation = i;
        ScriptEngineManager factory = new ScriptEngineManager();
        engine = factory.getEngineByName("JavaScript");
        engine.put("runeAgent", this);

    }

    public HashMap<String, Object> getModifiedObjects() {
        return modifiedObjects;
    }


    public void addRuneAgentListener(RuneAgentEventListener listener) {
        this.listeners.add(listener);
    }

    public void removeRuneAgentListener(RuneAgentEventListener listener) {
        this.listeners.remove(listener);
    }

    public void fireRuneAgentEvent(RuneAgentEvent event) {
        for (RuneAgentEventListener listener : listeners) {
            listener.onRuneAgentEvent(event);
        }
    }

    public void setAgentFrame(AgentFrame af) {
        if (agentFrame != null) {
            removeAgentFrame(af);
        }
        if (af instanceof RuneAgentEventListener) {
            this.addRuneAgentListener((RuneAgentEventListener) af);
        }
        this.agentFrame = af;
        this.fireRuneAgentEvent(new RuneAgentEvent(RuneAgentEventTypes.AGENT_FRAME_ADDED, af));
    }

    public AgentFrame getAgentFrame() {
        return this.agentFrame;
    }

    private void removeAgentFrame(AgentFrame af) {
        this.fireRuneAgentEvent(new RuneAgentEvent(RuneAgentEventTypes.AGENT_FRAME_REMOVED, af));
        if (af instanceof RuneAgentEventListener) {
            this.removeRuneAgentListener((RuneAgentEventListener) af);
        }
        agentFrame = null;
    }

    public void addTransformer(AgentTransformer transformer) {
        this.instrumentation.addTransformer(transformer);

        this.fireRuneAgentEvent(new RuneAgentEvent(RuneAgentEventTypes.AGENT_TRANSFORMER_ADDED, transformer));
    }

    public ScriptEngine getEngine() {
        return engine;
    }

    public RuneAgent getAgent() {
        return this;
    }
}
