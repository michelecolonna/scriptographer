var printDetails = true;

if (!this.print) {
	// if print is not defined, we're running within the Ant environment, let's define it here:
	this.print = function(str) {
		// replace white spaces with non breaking spaces so that they don't get trimmed away.
		// also replace empty with a non breaking space, otherwise it won't be printed
		echo.setMessage(str ? str.toString().replace(/\s/gi, '\xa0') : '\xa0');
		echo.execute();
	}
	// under Ant, we're not printing details:
	printDetails = false;
}

function loadJniClasses(dir, endsWithMask) {
	dir = new java.io.File(dir);
	var classes = {};
	filter = new java.io.FilenameFilter() {
		accept: function(dir, name) {
			return new java.lang.String(name).endsWith(endsWithMask ? endsWithMask : ".h");
		}
	}
	var lst = dir.listFiles(filter);
	for (var i=0; i < lst.length; i++) {
		collectJniClasses(lst[i], classes);
	}
	return classes;
}

/*
 * Parses .h files as generated by javah and creates a structure that contains descriptions of all existing classes and functions
 */
function collectJniClasses(file, classes) {
	var r = new java.io.BufferedReader(new java.io.FileReader(file)), s;
	while ((s = r.readLine()) != null) {
		var m = s.match(/Class:(?:\s*)(.*)/);
		if (m != null) {
			var cls = m[1], functions;
			// create a structure for this class if it doesn't exist already:
			if (classes[cls] == null) functions = [];
			else functions = classes[cls];
			// now read the function:
			// next line is the java function name:
			var func = { className: cls };
			m = r.readLine().match(/Method:(?:\s*)(.*)/);
			if (m != null) {
				func.javaName = m[1];
				// next line is the signature:
				m = r.readLine().match(/Signature:(?:\s*)(.*)/);
				if (m != null) {
					func.signature = m[1];
					// now skip the end line of the coment
					r.readLine();
					// and read the jni function line:
					var declaration = r.readLine();
					m = declaration.match(/(?:\s*)([^\s]*)(?:\s*)JNICALL(?:\s*)(.*)/);
					if (m != null) {
						func.declaration = declaration;
						func.returnType = m[1];
						func.jniName = m[2];
						// read the params:
						m = r.readLine().match(/(?:\s*)\((.*)\);/);
						if (m != null) {
							func.params = m[1];
							func.paramTypes = func.params.split(',');
							for (var i in func.paramTypes)
								func.paramTypes[i] = new java.lang.String(func.paramTypes[i]).trim();
	
							// put the function code for this function into the list of this class:
							functions.push(func);
						}
					}
				}
			}
			if (functions.length > 0) {
				classes[cls] = functions;
			}
		}
	}
}

/*
 * Parses a .cpp file previously generated by jni.js and creates a structure that contains descriptions of all existing functions
 */
function collectJniBodies(file) {
	var functions = {};
	var r = new java.io.BufferedReader(new java.io.FileReader(file)), s;
	while ((s = r.readLine()) != null) {
		var m = s.match(/JNIEXPORT(?:\s*)(.*)(?:\s*)JNICALL(?:\s*)(.*)\((.*)\)/);
		if (m != null) {
			var func = { returnType: new java.lang.String(m[1]).trim(), jniName: m[2], params: m[3] };
			var params = func.params.split(',');
			for (var i in params) {
				var m = params[i].match(/(?:\s*)([^\s]*)(?:\s*)([^\s]*)(?:\s*)/);
				if (m != null) {
					var param = m[1];
					// handle pointers correctly:
					if (m[2].charAt(0) == '*')
						param += ' *';
					params[i] = param;
				}
			}
			func.paramTypes = params;
			functions[func.jniName] = func;
		}
	}
	return functions;
}

function JniTypeReader(str) {
	this.string = str;
	this.position = 0;
}

JniTypeReader.prototype.readNext = function() {
	if (this.string && this.position < this.string.length) {
		var c = this.string.charAt(this.position);
		switch (c) {
			case 'Z':
				this.position++;
				return 'boolean';
			case 'B':
				this.position++;
				return 'byte';
			case 'C':
				this.position++;
				return 'char';
			case 'S':
				this.position++;
				return 'short';
			case 'I':
				this.position++;
				return 'int';
			case 'J':
				this.position++;
				return 'long';
			case 'F':
				this.position++;
				return 'float';
			case 'D':
				this.position++;
				return 'double';
			case 'V':
				this.position++;
				return 'void';
			case 'L': // fully qualified class
				var m = this.string.substring(this.position).match(/L([^;]*);/);
				if (m != null) {
					var cls = m[1];
					this.position += cls.length + 2;
					return cls.replace(/\//gi, '.');
				}
			case '[': // array
				this.position ++;
				return this.readNext() + '[]';
		}
	}
	return '';
}

JniTypeReader.prototype.readAll = function() {
	var list = [];
	do {
		var type = this.readNext();
		if (!type) break;
		list.push(type);
	} while (true);
	return list;
}

function registerNatives(srcDir, output) {
	// read the files in the directory
	var classes = loadJniClasses(srcDir);
	
	// now create the registerNatives file
	var file = new java.io.File(output);
	if (!file.exists()) file.createNewFile();
	var out = new java.io.PrintStream(new java.io.FileOutputStream(file));
	out.println('/* DO NOT EDIT THIS FILE - it is machine generated */');
	out.println('#include "stdHeaders.h"');
	out.println('#include "ScriptographerEngine.h"');
	out.println('#include <jni.h>');
	// generate includes for all the classes:
	out.println();
	out.println('/* Include headers for all classes */');
	for (var cls in classes) {
		out.println('#include "' + cls + '.h"');
	}
	out.println();
	
	// first the method lists:
	for (var cls in classes) {
		print(cls + ".h:");
		if (printDetails)
			print();
		out.println('/* Native methods for class ' + cls + ' */');
		out.println('static const JNINativeMethod ' + cls + '_methods[] = {');
		var functions = classes[cls];
		for (var i in functions) {
			var func = functions[i];
			out.println('\t{ "' + func.javaName + '", "' + func.signature + '", (void *) &' + func.jniName + ' }' + (i < functions.length - 1 ? ',' : ''));
			if (printDetails)
				print('    ' + func.javaName);
		}
		out.println('};');
		out.println();
		if (printDetails)
			print();
	}
	// and now the register methods:

	out.println('/* Registers natives for a given class an methods array */');
	out.println('void ScriptographerEngine::registerClassNatives(JNIEnv *env, const char *className, const JNINativeMethod *methods, int count) {');
	// use the internal ScriptographerEngine::findClass instead of JNI's because this one loads with the Scriptographer loader!
	out.println('\tjclass cls = findClass(env, className);');
	out.println('\tif (cls == NULL) {');
	out.println('\t\tchar msg[64];');
	out.println('\t\tsprintf(msg, "Cannot register natives for class %s: Class not found.", className);');
	out.println('\t\tthrow new StringException(msg);');
	out.println('\t}');
	out.println('\tjint err = env->RegisterNatives(cls, methods, count);');
	out.println('\tif (err != 0) {');
	out.println('\t\tchar msg[64];');
	out.println('\t\tsprintf(msg, "Cannot register natives for class %s.", className);');
	out.println('\t\tthrow new StringException(msg);');
	out.println('\t}');
	out.println('}');
	out.println();
	out.println('/* Registers natives for all classes, to be called from outside */');
	out.println('void ScriptographerEngine::registerNatives(JNIEnv *env) {');
	for (var cls in classes) {
		out.println('\tregisterClassNatives(env, "' + cls.replace(/_/gi, '/') + '", ' + cls + '_methods,');
		out.println('\t\tsizeof(' + cls + '_methods) / sizeof(JNINativeMethod));');
		out.println();
	}
	out.println('}');
}

function createJniBodies(srcDir, endsWithMask) {
	// read the files in the directory
	var classes = loadJniClasses(srcDir, endsWithMask);

	for (var cls in classes) {
		var file = new java.io.File(srcDir, cls + ".cpp");
		
		var existingBodies = file.exists() ? collectJniBodies(file) : null;
		
		print(cls + ".cpp: " + (existingBodies != null ? " appending..." : "creating..."));
		if (printDetails)
			print();
		file.createNewFile();
		var out = new java.io.PrintStream(new java.io.FileOutputStream(file, existingBodies != null));

		if (existingBodies == null) {
			// normal include: out.println('#include <jni.h>');
			// custom includes:
			out.println('#include "StdHeaders.h"');
			out.println('#include "ScriptographerEngine.h"');
			// custom end
			
			// include class header as well:
			out.println('#include "' + cls + '.h"');
			out.println();
			out.println('/*');
			out.println(' * ' + cls.replace(/_/gi, '.'));
			out.println(' */');
		}
	
		var functions = classes[cls];
		for (var i in functions) {
			var func = functions[i];
			// convert the signature to a correct java-like comment:
			var m = func.signature.match(/\((.*)\)(.*)/);
			if (m != null) {
				// use the JniTypeReader to conver the types:
				var params = m[1];
				var ret = m[2];
				
				params = new JniTypeReader(params).readAll();
				// add the param names, as used bellow for the C function:
				for (var i = 0; i < params.length; i++) {
					params[i] += ' ' + 'arg' + (i + 1);
				}
				var javaDecl = new JniTypeReader(ret).readNext() + ' ' + func.javaName + '(' + params.join(', ') + ')';

				var jniDecl = func.declaration + '(';
				for (var i in func.paramTypes) {
					var param = func.paramTypes[i];
					jniDecl += param;
					if (param == 'JNIEnv *') jniDecl += 'env';
					else {
						jniDecl += ' '; // no space needed for env!
						if (i < 2) {
							if (param == 'jclass') jniDecl += 'cls';
							else if (param == 'jobject') jniDecl += 'obj';
						} else jniDecl += 'arg' + (i - 1);
					} 
					if (i < func.paramTypes.length - 1) jniDecl += ', ';
				}
				jniDecl += ')';
				
				// now if we're appending to an existing file, only append this function if there is not alreaddy a body for it.
				// functions that have changed in the meantime will be appended again, causing a compiler-error that then can be
				// corrected manually.
				
				var append = true;
				
				if (existingBodies != null) {
					var body = existingBodies[func.jniName];
					if (body != null) {
						append = !(func.paramTypes.join().equals(body.paramTypes.join()) && func.returnType.equals(body.returnType));
						if (printDetails && !append)
							print('        ' + func.jniName + ':  already defined');
					}
					// now remove it and see what remains in the end:
					delete existingBodies[func.jniName];
				}
				if (append) {
					print('    ' + jniDecl + ': newly defined');
					
					out.println();
					out.println('/*');
					out.println(' * ' + javaDecl);
					out.println(' */');
					out.println(jniDecl + ' {');
					
					// custom function definition
					out.println('\ttry {');
					out.println('\t\t// TODO: define ' + func.javaName);
					out.println('\t} EXCEPTION_CONVERT(env)');
					// custom end
					
					switch (ret.charAt(0)) {
						case 'V': // void
							// do nothing
							break;
						case 'Z': // boolean
							out.println('\treturn JNI_FALSE;');
							break;
						case 'C': //
							out.println('\treturn \' \';');
							break;
						case 'L':
						case '[':
							out.println('\treturn NULL;');
							break;
						case 'F':
						case 'D':
							out.println('\treturn 0.0;');
							break;
						default:
							out.println('\treturn 0;');
							break;
					}
					out.println('}');
				}
			}
		}
		if (existingBodies != null) {
			var first = true;
			for (var n in existingBodies) {
				if (first) {
					print();
					first = false;
				}
				print('    --> ' + n + ':  no longer needed');
			}
		}
		if (printDetails)
			print();
	}
}
