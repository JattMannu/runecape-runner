/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.runescape.agent.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Type;
import com.runescape.agent.transformer.AgentTransformer;

/**
 *
 * @author unsignedbyte
 */
public class ClassModifier extends Identifiable {

    private final String className;
    private final String interfaceName;
    private final ArrayList<MethodWrapper> methodWrapper;
    private final ArrayList<FieldWrapper> fieldWrapper;
    private InstructionFactory factory;
    private ConstantPoolGen cpg;
    private static final ObjectType STRING_BUILDER_TYPE = new ObjectType("java.lang.StringBuilder");
    private final ArrayList<TransformerScript> scripts;
    private String returnClassName;

    public ClassModifier(String identity, String className, String interfaceName) {
        super(identity);
        this.className = className;
        this.interfaceName = interfaceName;
        this.methodWrapper = new ArrayList<MethodWrapper>();
        this.fieldWrapper = new ArrayList<FieldWrapper>();
        this.scripts = new ArrayList<TransformerScript>();
    }

    public String getClassName() {
        return className;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void addMethodWrapper(MethodWrapper m) {

        this.methodWrapper.add(m);
    }

    public void addFieldWrapper(FieldWrapper f) {
        this.fieldWrapper.add(f);
    }

    public void addScript(TransformerScript s) {
        this.scripts.add(s);
    }

    public void setReturnClassName(String s) {
        this.returnClassName = s;
    }

    public byte[] transform(ClassGen classGen, AgentTransformer transformer) {
        if (getInterfaceName() != null) {
            classGen.addInterface(getInterfaceName());
        }
        cpg = classGen.getConstantPool();

        factory = new InstructionFactory(classGen, cpg);
        for (FieldWrapper fw : fieldWrapper) {
            addFieldGetter(classGen, fw);
            addFieldSetter(classGen, fw);
            transformer.getAgent().getModifiedObjects().put(fw.getIdentity(), fw);
            System.out.println(fw.identity + " => hooked");
        }
        for (MethodWrapper mw : methodWrapper) {
            addMethodWrap(classGen, mw, transformer);
            transformer.getAgent().getModifiedObjects().put(mw.getIdentity(), mw);
            System.out.println(mw.identity + " => hooked");
        }
        for (TransformerScript s : scripts) {
            try {
                transformer.getAgent().getEngine().put("classGen", classGen);
                transformer.getAgent().getEngine().put("scriptObject", s);
                transformer.getAgent().getEngine().eval(new FileReader(s.getScript()));
            } catch (ScriptException ex) {
                Logger.getLogger(ClassModifier.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ClassModifier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            classGen.getJavaClass().dump(identity + ".class");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return classGen.getJavaClass().getBytes();
    }

    private void addFieldGetter(ClassGen classGen, FieldWrapper fw) {

        InstructionList il = new InstructionList();
        MethodGen mg = new MethodGen(Constants.ACC_PUBLIC, getType(fw.getCastType() == null ? fw.getFieldType() : fw.getCastType()), Type.NO_ARGS, null, "get" + fw.getSuffixMethodName(), classGen.getClassName(), il, cpg);
        il.append(new ALOAD(0));
        if (fw.isStatic()) {
            il.append(factory.createGetStatic(className, fw.getFieldName(), getType(fw.getFieldType())));
        } else {
            il.append(factory.createGetField(className, fw.getFieldName(), getType(fw.getFieldType())));
        }
        il.append(InstructionFactory.createReturn(getType(fw.getFieldType())));
        mg.setMaxLocals();
        mg.setMaxStack();
        classGen.addMethod(mg.getMethod());
        il.dispose();

    }

    private void addFieldSetter(ClassGen classGen, FieldWrapper fw) {
        InstructionList il = new InstructionList();
        MethodGen mg = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, new Type[]{getType(fw.getFieldType())}, new String[]{"o"}, "set" + fw.getSuffixMethodName(), classGen.getClassName(), il, cpg);
        il.append(new ALOAD(0));
        il.append(mg.getArgumentType(0).equals(Type.INT) ? new ILOAD(1) : new ALOAD(1));
        if (fw.isStatic()) {
            il.append(factory.createPutStatic(className, fw.getFieldName(), mg.getArgumentType(0)));
        } else {
            il.append(factory.createPutField(className, fw.getFieldName(), mg.getArgumentType(0)));
        }

        il.append(new RETURN());
        mg.setMaxLocals();
        mg.setMaxStack();
        classGen.addMethod(mg.getMethod());
        il.dispose();
    }

    private Type getType(String fieldType) {
        if (fieldType.contains("[]")) {
            return new ArrayType(fieldType.replace("[]", ""), 1);
        }
        if (fieldType.equals("int")) {
            return Type.INT;
        } else if (fieldType.equals("long")) {
            return Type.LONG;
        } else {
            return new ObjectType(fieldType);
        }
    }

    private void addMethodWrap(ClassGen classGen, MethodWrapper m, AgentTransformer transformer) {

        InstructionList list = new InstructionList();
        Type[] types = Type.getArgumentTypes(m.getSignature());
        String[] argName = new String[types.length];
        for (int i = 0; i < argName.length; i++) {
            argName[i] = "v" + i;
        }
        Type type = m.getReturnType() == null ? Type.VOID : getType(m.getReturnType());
        MethodGen methodGen
                = new MethodGen(Constants.ACC_PUBLIC, type, types,
                        argName, m.getIdentity(), classGen.getClassName(), list, cpg);
        list.append(InstructionFactory.createLoad(Type.OBJECT, 0));
        for (int i = 0; i < types.length; i++) {

            list.append(InstructionFactory.createLoad(types[i], 1 + i));
        }
        list.append(factory.createInvoke(classGen.getClassName(), m.getMethodName(), type, methodGen.getArgumentTypes(), Constants.INVOKEVIRTUAL));
        list.append(InstructionFactory.createReturn(type));
        methodGen.setMaxStack();
        methodGen.setMaxLocals();
        classGen.addMethod(methodGen.getMethod());
        list.dispose();
        if (m.getSpecial() != null) {
            list = new InstructionList();
            types = Type.getArgumentTypes(m.getSignature());
            argName = new String[types.length];
            for (int i = 0; i < argName.length; i++) {
                argName[i] = "v" + i;
            }
            type = m.getReturnType() == null ? Type.VOID : getType(m.getReturnType());
             methodGen
                    = new MethodGen(Constants.ACC_PUBLIC, type, types,
                            argName, m.getMethodName(), classGen.getClassName(), list, cpg);
         
            list.append(InstructionFactory.createLoad(Type.OBJECT, 0));
            for (int i = 0; i < types.length; i++) {

                list.append(InstructionFactory.createLoad(types[i], 1 + i));
            }
       
            list.append(factory.createInvoke(m.getSpecial(), m.getMethodName(), type, methodGen.getArgumentTypes(), Constants.INVOKESPECIAL));
            list.append(InstructionFactory.createReturn(type));
            methodGen.setMaxStack();
            methodGen.setMaxLocals();
            classGen.addMethod(methodGen.getMethod());
   
        
            list.dispose();
        }
            
        if (m.doLogging() && returnClassName != null) {
            String sig = m.getSignature();
            Method e =  classGen.containsMethod(m.getMethodName(), sig);
     
            methodGen = new MethodGen(e, classGen.getClassName(), cpg);
            list = methodGen.getInstructionList();
            InstructionList b = new InstructionList();
            b.append(factory.createNew("java.lang.StringBuilder"));
            b.append(new DUP());
            b.append(factory.createInvoke("java.lang.StringBuilder", "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
            b.append(factory.createConstant(m.getIdentity() + "("));
            b.append(factory.createInvoke("java.lang.StringBuilder", "append", STRING_BUILDER_TYPE, new Type[]{Type.STRING}, Constants.INVOKEVIRTUAL));

            Type[] t = methodGen.getArgumentTypes();

            for (int i = 0; i < t.length; i++) {
                b.append(factory.createLoad(t[i], 1 + i));
                if (t[i].equals(Type.BYTE)) {
                    t[i] = Type.INT;
                }

                b.append(factory.createInvoke("java.lang.StringBuilder", "append", STRING_BUILDER_TYPE, new Type[]{t[i]}, Constants.INVOKEVIRTUAL));
                if (i != (t.length - 1)) {
                    b.append(factory.createConstant(","));

                    b.append(factory.createInvoke("java.lang.StringBuilder", "append", STRING_BUILDER_TYPE, new Type[]{Type.STRING}, Constants.INVOKEVIRTUAL));
                }
            }

            b.append(factory.createConstant(")"));
            b.append(factory.createInvoke("java.lang.StringBuilder", "append", STRING_BUILDER_TYPE, new Type[]{Type.STRING}, Constants.INVOKEVIRTUAL));
            b.append(factory.createInvoke("java.lang.StringBuilder", "toString", Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
            b.append(factory.createInvoke(returnClassName, "log", Type.VOID, new Type[]{Type.STRING}, Constants.INVOKESTATIC));

            list.insert(b);

            methodGen.setMaxStack();
            methodGen.setMaxLocals();

            classGen.replaceMethod(e, methodGen.getMethod());

            b.dispose();
            list.dispose();
            transformer.getAgent().getModifiedObjects().put(m.getIdentity(), m.getMethodName());
        }
    }
}
