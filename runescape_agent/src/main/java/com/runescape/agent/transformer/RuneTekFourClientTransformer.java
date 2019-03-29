/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.runescape.agent.transformer;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import com.runescape.agent.util.ClassModifier;

/**
 *
 * @author unsignedbyte
 */
public class RuneTekFourClientTransformer extends ClassModifier {

    private final ObjectType isaac;

    public RuneTekFourClientTransformer(String identity, String className, String interfaceName, String isaac) {
        super(identity, className, interfaceName);
        this.isaac = new ObjectType(isaac);
    }

    @Override
    public byte[] transform(ClassGen classGen, AgentTransformer transformer) {
        super.transform(classGen, transformer);

        for (Method m : classGen.getMethods()) {
            MethodGen mg = new MethodGen(m, classGen.getClassName(), classGen.getConstantPool());
            InstructionList il = mg.getInstructionList();
            if(il == null){
                continue;
            }
            for (InstructionHandle h : il.getInstructionHandles()) {
                Instruction x = h.getInstruction();
                if (x instanceof NEW) {
                    Type lct = ((NEW) x).getLoadClassType(mg.getConstantPool());
                    if (lct.equals(isaac)) {
                        Instruction prev = h.getPrev().getInstruction();
                        if (prev instanceof GETFIELD || prev instanceof GETSTATIC) {
                            insertTransformerHook(classGen, mg, prev, transformer);
                        }
                    }
                }
            }

        }

        return classGen.getJavaClass().getBytes();
    }


    private void insertTransformerHook(ClassGen classGen, MethodGen mg, Instruction instruction, AgentTransformer transformer) {
        classGen.removeMethod(mg.getMethod());
        InstructionList init = new InstructionList();
        InstructionFactory factory = new InstructionFactory(classGen, classGen.getConstantPool());
        MethodGen mgn = new MethodGen(mg.getMethod(), classGen.getClassName(), classGen.getConstantPool());
        InstructionList methodInstruct = mgn.getInstructionList();
        if (instruction instanceof GETSTATIC) {
            GETSTATIC s = ((GETSTATIC) instruction);

            init.append(instruction);
        } else {
            init.append(new ALOAD(0));
            GETFIELD s = ((GETFIELD) instruction);

            init.append(instruction);
        }
        init.append(factory.createInvoke(transformer.getClass().getCanonicalName(), "setOutStream", Type.VOID, new Type[]{ObjectType.OBJECT}, Constants.INVOKESTATIC));
        methodInstruct.insert(methodInstruct.getStart(), init);
        mgn.setMaxLocals();
        mgn.setMaxStack();
        transformer.getAgent().getModifiedObjects().put("OutStream Hook", true);
        classGen.addMethod(mgn.getMethod());
    }
}
