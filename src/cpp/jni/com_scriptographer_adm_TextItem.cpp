/*
 * Scriptographer
 *
 * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
 *
 * Copyright (c) 2002-2005 Juerg Lehni, http://www.scratchdisk.com.
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
 * $RCSfile: com_scriptographer_adm_TextItem.cpp,v $
 * $Author: lehni $
 * $Revision: 1.5 $
 * $Date: 2006/05/30 16:03:40 $
 */
 
#include "stdHeaders.h"
#include "ScriptographerEngine.h"
#include "com_scriptographer_adm_TextItem.h"

/*
 * com.scriptographer.adm.TextItem
 */

/*
 * void setText(java.lang.String text)
 */
JNIEXPORT void JNICALL Java_com_scriptographer_adm_TextItem_setText(JNIEnv *env, jobject obj, jstring text) {
	try {
	    ADMItemRef item = gEngine->getItemRef(env, obj);
		ASUnicode *chars = gEngine->convertString_ASUnicode(env, text);
		sADMItem->SetTextW(item, chars);
		delete chars;
	} EXCEPTION_CONVERT(env)
}

/*
 * java.lang.String getText()
 */
JNIEXPORT jstring JNICALL Java_com_scriptographer_adm_TextItem_getText(JNIEnv *env, jobject obj) {
	try {
	    ADMItemRef item = gEngine->getItemRef(env, obj);
		long len = sADMItem->GetTextLength(item);
		ASUnicode *chars = new ASUnicode[len];
		sADMItem->GetTextW(item, chars, len);
		jstring res = gEngine->convertString(env, chars, len);
		delete chars;
		return res;
	} EXCEPTION_CONVERT(env)
	return NULL;
}
