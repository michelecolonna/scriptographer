/*
 * Scriptographer
 *
 * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
 *
 * Copyright (c) 2002-2010 Juerg Lehni, http://www.scratchdisk.com.
 * All rights reserved.
 *
 * Please visit http://scriptographer.org/ for updates and contact.
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
 */

script.keepAlive = true;
script.showProgress = false;

mapJavaClass(java.io.File, File);

importPackage(Packages.com.scriptographer);
importPackage(Packages.com.scratchdisk.script);
importPackage(Packages.com.scriptographer.script);

var buttonSize = new Size(27, 17);
var lineHeight = 17;
var lineBreak = java.lang.System.getProperty('line.separator');

// Script Locations
var examplesDirectory = new File(ScriptographerEngine.pluginDirectory, 'Examples');
var scriptDirectory = null;

function getImage(filename) {
	return new Image(new File(script.directory, 'resources/' + filename));
}

var firstRun = !script.preferences.accepted;

if (firstRun) {
	include('license.js');
	script.preferences.accepted = licenseDialog.doModal() == licenseDialog.defaultItem;
}

if (script.preferences.accepted) {
	include('console.js');
	include('about.js');
	include('main.js');

	if (!script.preferences.installed) {
		// include('install.js');
		script.preferences.installed = true;
	}
}
