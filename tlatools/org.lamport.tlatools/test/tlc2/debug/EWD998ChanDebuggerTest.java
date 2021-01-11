/*******************************************************************************
 * Copyright (c) 2021 Microsoft Research. All rights reserved. 
 *
 * The MIT License (MIT)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/
package tlc2.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.Variable;
import org.junit.Test;

import tla2sany.semantic.OpDeclNode;
import tlc2.TLCGlobals;
import tlc2.output.EC;
import tlc2.tool.impl.Tool;
import tlc2.util.Context;
import tlc2.value.impl.IntValue;

public class EWD998ChanDebuggerTest extends TLCDebuggerTestCase {

	private static final String UTILS = "Utils";
	private static final String FOLDER = "EWD998";
	private static final String RM = "EWD998Chan";
	private static final String MDL = "EWD998Chan";

	public EWD998ChanDebuggerTest() {
		super(MDL, FOLDER, EC.ExitStatus.SUCCESS, createBreakpointArgument(UTILS,12));
	}

	@Test
	public void testSpec() throws Exception {
		StackFrame[] stackFrames = debugger.stackTrace();
		
		// ASSUME in EWD998Chan
		assertEquals(1, stackFrames.length);
		assertTLCFrame(stackFrames[0], 11, 11, RM);
		// prefix depends on where the tests execute.
		assertTrue(stackFrames[0].getSource().getPath().endsWith("test-model/EWD998/EWD998Chan.tla"));
		stackFrames = debugger.stepIn();
		assertEquals(2, stackFrames.length);
		assertTLCFrame(stackFrames[1], 11, 11, RM);
		assertTLCFrame(stackFrames[0], 11, 11, RM);
		
		// The order of vars is expected to be deterministic across tests (local,
		// because TLCState.Empty is null during ctor-time).
		final Tool tool = (Tool) TLCGlobals.mainChecker.tool;
		final OpDeclNode[] vars = tool.getSpecProcessor().getVariablesNodes();
		
		// Debug an operator that is evaluated as part of the refinement mapping and know to
		// consist of a bunch of LazyValues.  LazyValues are tricky because the debugger
		// unlazies them, which has to be ignored by DebugTool.  Otherwise, the debugger
		// debugs itself and deadlocks.
		debugger.setBreakpoints(UTILS, 13);
		stackFrames = debugger.continue_();
		
		assertEquals(15, stackFrames.length);
		assertTLCStateFrame(stackFrames[14], 43, 49, RM, vars);
		assertTLCStateFrame(stackFrames[13], 43, 43, RM, vars);
		assertTLCStateFrame(stackFrames[12], 44, 46, RM, vars[0], vars[1], vars[3]);
		assertTLCStateFrame(stackFrames[11], 48, 48, RM, vars[0], vars[1]);
		assertTLCStateFrame(stackFrames[10], 49, 49, RM, vars[1]);
		assertTLCStateFrame(stackFrames[9], 49, 49, RM);
		assertTLCStateFrame(stackFrames[8], 166, 180, RM);

		// action, counter, color, pending, and token are part of the context because
		// this is debugging the refinement mapping.
		Map<String, String> allVariables = new HashMap<>();
		allVariables.put("pending", "(0 :> 0 @@ 1 :> 0 @@ 2 :> 0)");
		allVariables.put("token", "[pos |-> 0, q |-> 0, color |-> \"black\"]");
		allVariables.put("counter", "(0 :> 0 @@ 1 :> 0 @@ 2 :> 0)");
		allVariables.put("N", "3");
		allVariables.put("active", "(0 :> FALSE @@ 1 :> FALSE @@ 2 :> FALSE)");
		allVariables.put("color", "(0 :> \"white\" @@ 1 :> \"white\" @@ 2 :> \"white\")");
		assertTLCStateFrame(stackFrames[7], 150, 162, FOLDER, allVariables);
		assertTLCStateFrame(stackFrames[6], 150, 150, FOLDER, allVariables);
		assertTLCStateFrame(stackFrames[5], 150, 150, FOLDER, allVariables);
		assertTLCStateFrame(stackFrames[4], 133, 133, FOLDER, allVariables);
		
		allVariables.put("op", "sum(a,b) == a+b");
		allVariables.put("fun", "(0 :> 0 @@ 1 :> 0 @@ 2 :> 0)");
		allVariables.put("from", "0");
		allVariables.put("to", "2");
		allVariables.put("base", "0");
		assertTLCStateFrame(stackFrames[3], 11, 14, UTILS, allVariables);
		allVariables.put("reduced", "(0 :> 0 @@ 1 :> 0 @@ 2 :> 0)");
		assertTLCStateFrame(stackFrames[2], 14, 14, UTILS, allVariables);
		allVariables.put("i", "2");
		assertTLCStateFrame(stackFrames[1], 12, 13, UTILS, allVariables);
		assertTLCStateFrame(stackFrames[0], 13, 13, UTILS, allVariables);
		
		// 88888888888888888888888888888888888888888888888888888888888888 //
		
		// Step through the evaluation of a mildly complex expression. 
		debugger.setBreakpoints(RM, 119);
		stackFrames = debugger.continue_();
		assertEquals(8, stackFrames.length);
		Context context = Context.Empty.cons(null, IntValue.ValOne).cons(null, IntValue.ValOne).cons(null, IntValue.ValOne);
		assertTLCActionFrame(stackFrames[0], 119, 119, RM, context, vars[3]);

		stackFrames = debugger.stepIn();
		assertEquals(9, stackFrames.length);
		assertTLCActionFrame(stackFrames[0], 119, 119, RM, context, vars[3]);

		stackFrames = debugger.stepIn();
		assertEquals(10, stackFrames.length);
		Set<Variable> variables = new HashSet<>();
		variables.add(createVariable("i","1","IntValue"));
		variables.add(createVariable("j","1","IntValue"));
		variables.add(createVariable("@","<<[type |-> \"pl\"]>>","TupleValue"));
		variables.add(createVariable("s","<<[type |-> \"pl\"]>>","TupleValue"));
		assertTLCActionFrame(stackFrames[0], 119, 119, RM, variables, vars[3]);

		stackFrames = debugger.stepIn();
		assertEquals(11, stackFrames.length);
		assertTLCActionFrame(stackFrames[0], 29, 29, UTILS, variables, vars[3]);

		stackFrames = debugger.stepIn(6);
		assertEquals(9, stackFrames.length);
		variables = new HashSet<>();
		variables.add(createVariable("i","1","IntValue"));
		variables.add(createVariable("j","1","IntValue"));
		assertTLCActionFrame(stackFrames[0], 120, 120, RM, variables);
		
		// Remove all breakpoints and run the spec to completion.
		debugger.unsetBreakpoints();
		debugger.continue_();
	}
}
