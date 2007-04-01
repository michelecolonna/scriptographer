/*
 * Scriptographer
 *
 * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
 *
 * Copyright (c) 2002-2007 Juerg Lehni, http://www.scratchdisk.com.
 * All rights reserved.
 *
 * Please visit http://scriptographer.com/ for updates and contact.
 *
 * -- GPL LICENSE NOTICE --
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * -- GPL LICENSE NOTICE --
 * 
 * File created on Feb 19, 2007.
 *
 * $Id: $
 */

package com.scriptographer.script.rhino;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.Wrapper;

import com.scriptographer.ScriptographerEngine;
import com.scriptographer.ai.Matrix;
import com.scriptographer.ai.Point;
import com.scriptographer.ai.Rectangle;
import com.scriptographer.ai.SegmentPoint;
import com.scriptographer.ai.Style;
import com.scriptographer.script.Script;
import com.scriptographer.script.ScriptCanceledException;
import com.scriptographer.script.ScriptMethod;
import com.scriptographer.script.ScriptScope;
import com.scriptographer.script.ScriptEngine;
import com.scriptographer.script.ScriptException;
import com.scriptographer.util.ReadOnlyList;
import com.scriptographer.util.StringUtils;
import com.scriptographer.util.WeakIdentityHashMap;

/**
 * @author lehni
 *
 */
public class RhinoEngine extends ScriptEngine {
	protected Context context;
	protected GlobalObject global;
	protected Debugger debugger = null;

	public RhinoEngine() {
		super("JavaScript", new String[] { "js" });
		// initialize the JS stuff
		RhinoContextFactory factory = new RhinoContextFactory();
		RhinoContextFactory.initGlobal(factory);

		// The debugger needs to be created before the context, otherwise
		// notification won't work
//		debugger = new Debugger();
//		debugger.attachTo(factory);
		
		context = Context.enter();
		global = new GlobalObject(context);

		// now define the scope provider. Things are a bit intertwingled here...
//		debugger.setScopeProvider(global);
	}
	
	protected Script compileScript(File file)
			throws RhinoScriptException, IOException {
		FileReader in = null;
		try {
			in = new FileReader(file);
			return new RhinoScript(context.compileReader(
					in, file.getPath(), 1, null), file);
		} catch (RhinoException e) {
			throw new RhinoScriptException(e);
		} finally {
			if (in != null)
				in.close();
		}
	}

	public void evaluate(String string, ScriptScope scope) throws RhinoScriptException {
		try {
			// TODO: typecast to JsContext can be wrong, e.g. when calling
			// from another language
			this.context.evaluateString(((RhinoScope) scope).getScope(), string,
					null, 1, null);
		} catch (RhinoException e) {
			throw new RhinoScriptException(e);
		}
	}

	public ScriptScope createScope() {
		return new RhinoScope();
	}
	
	private Scriptable getScriptable(Object obj) {
		if (obj instanceof Scriptable) {
			return (Scriptable) obj;
		} else {
			return context.getWrapFactory().wrapAsJavaObject(context, global, obj, obj.getClass());
		}
	}

	public ScriptScope getScope(Object obj) {
		// Set global as the parent scope, so Tool buttons work.
		// TODO: this might not work for Jython or JRuby. Find a better 
		// way to handle this
		Scriptable scriptable = getScriptable(obj);
		scriptable.setParentScope(global);
		return new RhinoScope(scriptable);
	}

	private class RhinoScript extends Script {
		private org.mozilla.javascript.Script script;

		public RhinoScript(org.mozilla.javascript.Script script, File file) {
			super(file);
			this.script = script;
		}
		
		public ScriptEngine getEngine() {
			return RhinoEngine.this;
		}

		public Object executeScript(ScriptScope scope) throws ScriptException {
			try {
				// TODO: typecast to JsContext can be wrong, e.g. when calling
				// from another language
				Object ret = script.exec(context, ((RhinoScope) scope).getScope());
				if (ret instanceof Wrapper)
					ret = ((Wrapper) ret).unwrap();
				return ret;
			} catch (RhinoException re) {
				throw new RhinoScriptException(re);
			}
		}
	}
	
	private class RhinoMethod extends ScriptMethod {
		Function function;
		
		RhinoMethod(Function function) {
			this.function = function;
		}
		
		public Object call(Object obj, Object[] args) throws RhinoScriptException {
			// Retrieve wrapper object for the native java object, and call the
			// function on it.
			try {
				Scriptable scope = getScriptable(obj);
				Object ret = function.call(context, global, scope, args);
				// unwrap if the return value is a native java object:
				if (ret instanceof Wrapper)
					ret = ((Wrapper) ret).unwrap();
				return ret;
			} catch (RhinoException re) {
				throw new RhinoScriptException(re);
			}
		}

		public Function getFunction() {
			return function;
		}

		public boolean toBoolean(Object object) {
			return ScriptRuntime.toBoolean(object);
		}

		public int toInt(Object object) {
			return ScriptRuntime.toInt32(object);
		}
	}

	private class RhinoScope extends ScriptScope {
		private Scriptable scope;

		/**
		 * Wrapps an existing scope
		 * 
		 * @param scope
		 */
		public RhinoScope(Scriptable scope) {
			this.scope = scope;
		}
		
		public RhinoScope() {
			scope = new NativeObject();
			scope.setPrototype(global);
			scope.setParentScope(null);
		}
		
		public Scriptable getScope() {
			return scope;
		}

		public Object get(String name) {
			Object obj = scope.get(name, scope);
			if (obj == Scriptable.NOT_FOUND) return null;
			else if (obj instanceof Function) return new RhinoMethod((Function) obj);
			else if (obj instanceof Wrapper) return ((Wrapper) obj).unwrap();
			else return obj;
		}

		public Object put(String name, Object value, boolean readOnly) {
			Object prev = this.get(name);
			if (readOnly && scope instanceof ScriptableObject)
				((ScriptableObject) scope).defineProperty(name, value,
				ScriptableObject.READONLY | ScriptableObject.DONTENUM);
			else
				scope.put(name, scope, value);
			return prev;
		}
	}

	/**
	 * ScriptException for Rhino, preferably called RhinoException, but
	 * that's already used by Rhino (org.mozilla.javascript.RhinoException).
	 */
	private static class RhinoScriptException extends ScriptException {

		private static String formatMessage(Throwable t) {
			RhinoException re = t instanceof RhinoException ? (RhinoException) t
					: new WrappedException(t);

				String basePath =
					ScriptographerEngine.getScriptDirectory().getAbsolutePath();

				StringWriter buf = new StringWriter();
				PrintWriter writer = new PrintWriter(buf);

				writer.println(re.details());
				writer.print(StringUtils.replace(StringUtils.replace(
					re.getScriptStackTrace(), basePath, ""), "\t", "    "));
				
				return buf.toString();
		}

		public RhinoScriptException(Throwable cause) {
			super(formatMessage(cause), cause);
		}
	}
	
	private class RhinoContextFactory extends ContextFactory {
		
		protected boolean hasFeature(Context cx, int featureIndex) {
			switch (featureIndex) {
				case Context.FEATURE_E4X:
					return false;
				case Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME:
					return true;
				case Context.FEATURE_DYNAMIC_SCOPE:
					return false;
			}
			return super.hasFeature(cx, featureIndex);
		}

	    protected Context makeContext() {
	        Context context = new Context();
			context.setApplicationClassLoader(getClass().getClassLoader());
			context.setWrapFactory(new RhinoWrapFactory());

//			context.setOptimizationLevel(9);
	        // Use pure interpreter mode to allow for
	        // observeInstructionCount(Context, int) to work
	        context.setOptimizationLevel(-1);
	        // Make Rhino runtime to call observeInstructionCount
	        // each 10000 bytecode instructions
	        context.setInstructionObserverThreshold(20000);
	        
	        return context;
	    }
	    
	    protected void observeInstructionCount(Context cx, int instructionCount) {
			if (!ScriptographerEngine.updateProgress())
				throw new ScriptCanceledException();
		}
	}

	private class RhinoWrapFactory extends WrapFactory {
		private WeakIdentityHashMap wrappers = new WeakIdentityHashMap();
		
		public RhinoWrapFactory() {
			this.setJavaPrimitiveWrap(true);
		}

		public Object wrap(Context cx, Scriptable scope, Object obj,
				Class staticType) {
			// these are not wrappers, the java return types are simply converted to
			// these scriptographer types and wrapped afterwards:
			if (obj instanceof java.awt.geom.Rectangle2D
				&& !(obj instanceof Rectangle)) {
				obj = new Rectangle((java.awt.geom.Rectangle2D) obj);
			} else if (obj instanceof java.awt.geom.Point2D
				&& !(obj instanceof Point)) {
				obj = new Point((java.awt.geom.Point2D) obj);
			} else if (obj instanceof java.awt.geom.AffineTransform
				&& !(obj instanceof Matrix)) {
				obj = new Matrix((java.awt.geom.AffineTransform) obj);
			} else if (obj instanceof java.awt.Dimension) {
				// TODO: expose Dimension to JS?
				obj = new Point((java.awt.Dimension) obj);
			} else if (obj instanceof RhinoMethod) {
				// Handle the ScriptFunction special case, return the unboxed
				// function value.
				obj = ((RhinoMethod) obj).getFunction();
			}
			return super.wrap(cx, scope, obj, staticType);
		}

		public Scriptable wrapAsJavaObject(Context cx, Scriptable scope,
				Object javaObj, Class staticType) {
			// keep track of wrappers so that if a given object needs to be
			// wrapped again, take the wrapper from the pool...
			// TODO: see wether this really makes sense or wether rewrapping
			// every time is the way to go
			Scriptable obj = (Scriptable) wrappers.get(javaObj);
			if (obj == null) {
				if (javaObj instanceof ReadOnlyList) {
					obj = new ListObject(scope, (ReadOnlyList) javaObj, staticType);
				} else if (javaObj instanceof Map) {
					obj = new MapObject(scope, (Map) javaObj, staticType);
				} else if (javaObj instanceof Style) {
					obj = new StyleObject(scope, (Style) javaObj, staticType);
				} else if (javaObj instanceof SegmentPoint) {
					obj = new SegmentPointWrapper(scope, (SegmentPoint) javaObj, staticType);
				} else {
					// The default for Scriptographer is unsealed
					obj = new ExtendedJavaObject(scope, javaObj, staticType, true);
				}
				wrappers.put(javaObj, obj);
			}
			return obj;
		}

		public Object coerceType(Class type, Object value) {
			// coerce native objects to maps when needed
			if (value instanceof NativeObject && Map.class.isAssignableFrom(type)) {
				return convertToMap((NativeObject) value);
			} else if (value instanceof Function && type == ScriptMethod.class) {
				return new RhinoMethod((Function) value);
			}
			return null;
		}

		protected Map convertToMap(NativeObject object) {
			HashMap map = new HashMap();
			Object[] ids = object.getIds();
			for (int i = 0; i < ids.length; i++) {
				Object id = ids[i];
				Object obj = id instanceof String ? object.get((String) id, object)
					: object.get(((Number) id).intValue(), object);
				map.put(id, convertObject(obj));
			}
			return map;
		}

		protected Object[] convertToArray(NativeArray array) {
			Object[] objects = new Object[(int) array.getLength()];
			for (int i = 0; i < objects.length; i++)
				objects[i] = convertObject(array.get(i, array));
			return objects;
		}

		protected Object convertObject(Object obj) {
			if (obj instanceof Wrapper) {
				return ((Wrapper) obj).unwrap();
			} else if (obj instanceof NativeArray) {
				return convertToArray((NativeArray) obj);
			} else if (obj instanceof NativeObject) {
				return convertToMap((NativeObject) obj);
			}
			return obj;
		}
	}
}