package uk.ac.manchester.tornado.drivers.cuda.graal.compiler;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.*;
import org.graalvm.compiler.core.common.CompressEncoding;
import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.core.common.calc.Condition;
import org.graalvm.compiler.core.common.spi.ForeignCallLinkage;
import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.lir.*;
import org.graalvm.compiler.lir.gen.LIRGenerationResult;
import org.graalvm.compiler.lir.gen.LIRGenerator;
import org.graalvm.compiler.phases.util.Providers;
import uk.ac.manchester.tornado.drivers.cuda.CUDATargetDescription;
import uk.ac.manchester.tornado.drivers.cuda.graal.PTXLIRKindTool;
import uk.ac.manchester.tornado.drivers.cuda.graal.asm.PTXAssembler.PTXNullaryOp;
import uk.ac.manchester.tornado.drivers.cuda.graal.asm.PTXAssembler.PTXUnaryOp;
import uk.ac.manchester.tornado.drivers.cuda.graal.lir.*;

import static uk.ac.manchester.tornado.api.exceptions.TornadoInternalError.unimplemented;
import static uk.ac.manchester.tornado.drivers.cuda.graal.asm.PTXAssembler.PTXTernaryOp.SETP;
import static uk.ac.manchester.tornado.drivers.cuda.graal.asm.PTXAssembler.PTXUnaryOp.BRA;
import static uk.ac.manchester.tornado.drivers.cuda.graal.asm.PTXAssembler.PTXUnaryOp.CONDITIONAL_BRA;
import static uk.ac.manchester.tornado.drivers.cuda.graal.lir.PTXLIRStmt.ExprStmt;
import static uk.ac.manchester.tornado.runtime.graal.compiler.TornadoCodeGenerator.trace;

public class PTXLIRGenerator extends LIRGenerator {
    private PTXGenTool ptxGenTool;

    public PTXLIRGenerator(Providers providers, LIRGenerationResult lirGenRes) {
        super(
                new PTXLIRKindTool((CUDATargetDescription) providers.getCodeCache().getTarget()),
                new PTXArithmeticTool(),
                new PTXMoveFactory(),
                providers,
                lirGenRes
        );

        ptxGenTool = new PTXGenTool(this);
    }

    @Override
    public PTXLIRKindTool getLIRKindTool() {
        return (PTXLIRKindTool) super.getLIRKindTool();
    }

    @Override
    public CUDATargetDescription target() {
        return (CUDATargetDescription) super.target();
    }

    @Override
    public PTXArithmeticTool getArithmetic() {
        return (PTXArithmeticTool) super.getArithmetic();
    }

    @Override
    public Value emitCompress(Value pointer, CompressEncoding encoding, boolean nonNull) {
        unimplemented();
        return null;
    }

    @Override
    public Value emitUncompress(Value pointer, CompressEncoding encoding, boolean nonNull) {
        unimplemented();
        return null;
    }

    @Override
    public void emitConvertNullToZero(AllocatableValue result, Value input) {
        unimplemented();
    }

    @Override
    public void emitConvertZeroToNull(AllocatableValue result, Value input) {
        unimplemented();
    }

    @Override
    public VirtualStackSlot allocateStackSlots(int slots) {
        unimplemented();
        return null;
    }

    @Override
    public Value emitReadCallerStackPointer(Stamp wordStamp) {
        unimplemented();
        return null;
    }

    @Override
    public Value emitReadReturnAddress(Stamp wordStamp, int returnAddressSize) {
        unimplemented();
        return null;
    }

    @Override
    public void emitZeroMemory(Value address, Value length, boolean isAligned) {
        unimplemented();
    }

    @Override
    public void emitSpeculationFence() {
        unimplemented();
    }

    @Override
    public <K extends ValueKind<K>> K toRegisterKind(K kind) {
        return kind;
    }

    @Override
    public void emitNullCheck(Value address, LIRFrameState state) {
        unimplemented();
    }

    @Override
    public Variable emitLogicCompareAndSwap(LIRKind accessKind, Value address, Value expectedValue, Value newValue,
                                            Value trueValue, Value falseValue) {
        unimplemented();
        return null;
    }

    @Override
    public Value emitValueCompareAndSwap(LIRKind accessKind, Value address, Value expectedValue, Value newValue) {
        unimplemented(); return null;
    }

    @Override
    public void emitDeoptimize(Value actionAndReason, Value failedSpeculation, LIRFrameState state) {
        unimplemented();
    }

    @Override
    public Variable emitAddress(AllocatableValue stackslot) {
        unimplemented(); return null;
    }

    @Override
    public void emitMembar(int barriers) {
        unimplemented();
    }

    @Override
    public void emitUnwind(Value operand) {
        unimplemented();
    }

    @Override
    public void emitReturn(JavaKind javaKind, Value input) {
        trace("emitReturn: input=%s", input);
        if (input != null) {
            LIRKind lirKind = LIRKind.value(input.getPlatformKind());
            ExprStmt stmt = new ExprStmt(new PTXUnary.Expr(PTXUnaryOp.RETURN, lirKind, input));
            append(stmt);
        } else {
            append(new ExprStmt(new PTXNullary.Expr(PTXNullaryOp.RETURN, LIRKind.Illegal)));
        }
    }

    @Override
    public void emitJump(LabelRef label) {
        unimplemented();
    }

    @Override
    public void emitCompareBranch(PlatformKind cmpKind,
                                  Value left,
                                  Value right,
                                  Condition cond,
                                  boolean unorderedIsTrue,
                                  LabelRef trueDestination,
                                  LabelRef falseDestination,
                                  double trueDestinationProbability) {

        LIRKind lirKind = LIRKind.value(cmpKind);
        Variable isTrue = newVariable(LIRKind.value(PTXKind.PRED));
        ExprStmt compare = new ExprStmt(new PTXTernary.Expr(SETP, lirKind, isTrue, left, right));
        ExprStmt branchTrue = new ExprStmt(new PTXUnary.Branch(CONDITIONAL_BRA, lirKind, isTrue, trueDestination));
        ExprStmt branchFalse = new ExprStmt(new PTXUnary.Branch(BRA, lirKind, null, falseDestination));

        append(compare);
        append(branchTrue);
        append(branchFalse);
    }

    @Override
    public void emitOverflowCheckBranch(LabelRef overflow, LabelRef noOverflow, LIRKind cmpKind,
                                        double overflowProbability) {
        unimplemented();
    }

    @Override
    public void emitIntegerTestBranch(Value left, Value right, LabelRef trueDestination, LabelRef falseDestination,
                                      double trueSuccessorProbability) {
        unimplemented();
    }

    @Override
    public Variable emitConditionalMove(PlatformKind cmpKind, Value leftVal, Value right, Condition cond,
                                        boolean unorderedIsTrue, Value trueValue, Value falseValue) {
        unimplemented(); return null;
    }

    @Override
    public Variable emitIntegerTestMove(Value leftVal, Value right, Value trueValue, Value falseValue) {
        unimplemented(); return null;
    }

    @Override
    protected void emitForeignCallOp(ForeignCallLinkage linkage, Value result, Value[] arguments, Value[] temps,
                                     LIRFrameState info) {
        unimplemented();
    }

    @Override
    public void emitStrategySwitch(SwitchStrategy strategy, Variable key, LabelRef[] keyTargets,
                           LabelRef defaultTarget) {
        unimplemented();
    }

    @Override
    public Variable emitByteSwap(Value operand) {
        unimplemented(); return null;
    }

    @Override
    public Variable emitArrayEquals(JavaKind kind, Value array1, Value array2, Value length, boolean directPointers) {
        unimplemented(); return null;
    }

    @Override
    public void emitPause() {
        unimplemented();
    }

    @Override
    public void emitPrefetchAllocate(Value address) {
        unimplemented();
    }

    @Override
    protected void emitTableSwitch(int lowKey, LabelRef defaultTarget, LabelRef[] targets, Value key) {
        unimplemented();
    }

    @Override
    protected JavaConstant zapValueForKind(PlatformKind kind) {
        unimplemented(); return null;
    }

    @Override
    public StandardOp.ZapRegistersOp createZapRegisters(Register[] zappedRegisters, JavaConstant[] zapValues) {
        unimplemented(); return null;
    }

    @Override
    public LIRInstruction createZapArgumentSpace(StackSlot[] zappedStack, JavaConstant[] zapValues) {
        unimplemented(); return null;
    }

    public PTXGenTool getPTXGenTool() {
        return ptxGenTool;
    }
}
