/*
 * Scriptographer
 * 
 * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
 * 
 * Copyright (c) 2004-2005 Juerg Lehni, http://www.scratchdisk.com.
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
 * File created on 04.11.2005.
 * 
 * $RCSfile: FontFamily.java,v $
 * $Author: lehni $
 * $Revision: 1.1 $
 * $Date: 2005/11/05 00:50:41 $
 */

package com.scriptographer.ai;

import com.scriptographer.util.ExtendedList;
import com.scriptographer.util.Lists;
import com.scriptographer.util.ReadOnlyList;
import com.scriptographer.util.SoftIntMap;
import com.scriptographer.util.StringIndexList;

public class FontFamily extends AIObject implements ReadOnlyList, StringIndexList {
	
	protected FontFamily(int handle) {
		super(handle);
	}
	
	private native String nativeGetName(int handle);	
	
	public  String getName() {
		return nativeGetName(handle);
	}
	
	public boolean isValid() {
		return getLength() > 0;
	}

	/* TODO: check AIFont.h for many more features (OpenType, glyph bounds, etc)
	AIAPI AIErr (*GetFullFontName)( AIFontKey font, char *fontName, short maxName );
	AIAPI AIErr (*GetPostScriptFontName)( AIFontKey fontKey, char* postScriptFontName, short maxName );
	AIAPI AIErr (*GetFontStyleName)( AIFontKey font, char *styleName, short maxName );
	AIAPI AIErr (*GetFontFamilyUIName)( AIFontKey font, char *familyName, short maxName );
	AIAPI AIErr (*GetFontStyleUIName)( AIFontKey font, char *styleName, short maxName );
	AIAPI AIErr (*GetTypefaceName)( AITypefaceKey typeface, char *name, short maxName );
	AIAPI AIErr (*GetUserFontName)( AIFontKey font, char *userfontName, short maxName );
	AIAPI AIErr (*GetUserFontUIName)( AIFontKey font, char *userfontName, short maxName );
	AIAPI AIErr (*GetUserFontUINameUnicode)( AIFontKey fontKey, ASUnicode* userfontName, long maxName );
	AIAPI AIErr (*GetFontFamilyUINameUnicode)( AIFontKey fontKey, ASUnicode* familyName, long maxName );
	AIAPI AIErr (*GetFontStyleUINameUnicode)( AIFontKey fontKey, ASUnicode* styleName, long maxName );
	*/

	private native int nativeGetLength(int handle);
	
	public int getLength() {
		return nativeGetLength(handle);
	}
	
	private static native int nativeGet(int handle, int index);

	public Object get(int index) {
		return FontWeight.wrapHandle(nativeGet(handle, index));
	}

	public Object get(String name) {
		if (name != null) {
			for (int i = getLength() - 1; i >= 0; i--) {
				FontWeight weight = (FontWeight) get(i);
				if (name.equals(weight.getName()))
					return weight;
			}
		}
		return null;
	}

	public boolean isEmpty() {
		return getLength() == 0;
	}

	public ExtendedList subList(int fromIndex, int toIndex) {
		return Lists.createSubList(this, fromIndex, toIndex);
	}
	
	// use a SoftIntMap to keep track of already wrapped families:
	private static SoftIntMap families = new SoftIntMap();
	
	protected static FontFamily wrapHandle(int handle) {
		if (handle == 0)
			return null;
		FontFamily family = (FontFamily) families.get(handle);
		if (family == null) {
			family = new FontFamily(handle);
			families.put(handle, family);
		}
		return family;
	}
}
