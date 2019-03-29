/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.runescape.agent.event;

/**
 *
 * @author unsignedbyte
 */
public class TransformerEvent extends RuneAgentEvent{
    private final String message;

    public TransformerEvent(Object source,String message) {
        super(RuneAgentEventTypes.TRANSFORMER_EVENT, source);
        this.message = message;
    }

    @Override
    public String toString() {
        return "Transformer => "+this.getSource()+" =>"+message;
    }
    
}
