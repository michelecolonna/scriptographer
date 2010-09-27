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
 * 
 * File created on Mar 21, 2008.
 */

package com.scriptographer.ai;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

import com.scratchdisk.list.List;

/**
 * @author lehni
 */
public abstract class PathItem extends Item {
	/**
	 * Wraps an AIArtHandle in a Path object
	 */
	protected PathItem(int handle, int docHandle, boolean created) {
		super(handle, docHandle, created);
	}

	/**
	 * Creates a path object of the given type. Used by CompoundPath
	 */
	protected PathItem(short type) {
		super(type);
	}

	/*
	 * Convert to and from Java2D (java.awt.geom)
	 */
	
	/**
	 * @jshide
	 */
	public abstract GeneralPath toShape();

	/**
	 * @jshide
	 */
	public abstract void append(PathIterator iter, boolean connect);

	/*
	 *  PostScript-like interface: moveTo, lineTo, curveTo, arcTo
	 */

	/**
	 * {@grouptitle PostScript-style drawing commands}
	 */
	public abstract void moveTo(double x, double y);	

	public void moveTo(Point pt) {
		if (pt == null) moveTo(0, 0);
		else moveTo(pt.x, pt.y);
	}

	public abstract void lineTo(double x, double y);

	public void lineTo(Point pt) {
		if (pt == null) lineTo(0, 0);
		else lineTo(pt.x, pt.y);
	}

	public abstract void curveTo(double handle1X, double handle1Y,
			double handle2X, double handle2Y,
			double endX, double endY);

	public void curveTo(Point handle1, Point handle2, Point end) {
		curveTo(handle1 != null ? handle1.x : 0,
				handle1 != null ? handle1.y : 0,
				handle2 != null ? handle2.x : 0,
				handle2 != null ? handle2.y : 0,
				end != null ? end.x : 0,
				end != null ? end.y : 0);
	}

	public abstract void curveTo(double handleX, double handleY,
			double endX, double endY);

	public void curveTo(Point handle, Point end) {
		curveTo(handle != null ? handle.x : 0, handle != null ? handle.y : 0,
				end != null ? end.x : 0, end != null ? end.y : 0);
	}

	public abstract void arcTo(double endX, double endY);

	public void arcTo(Point end) {
		arcTo(end != null ? end.x : 0, end != null ? end.y : 0);
	}

	public abstract void curveThrough(double middleX, double middleY,
			double endX, double endY, double t);

	public void curveThrough(double middleX, double middleY,
			double endX, double endY) {
		curveThrough(middleX, middleY, endX, endY, 0.5);
	}

	public void curveThrough(Point middle, Point end, double t) {
		curveThrough(middle != null ? middle.x : 0, middle != null ? middle.y : 0,
				end != null ? end.x : 0, end != null ? end.y : 0, t);
	}

	public void curveThrough(Point middle, Point end) {
		curveThrough(middle, end, 0.5);
	}

	public abstract void arcThrough(double middleX, double middleY,
			double endX, double endY);

	public void arcThrough(Point middle, Point end) {
		arcThrough(middle != null ? middle.x : 0, middle != null ? middle.y : 0,
				end != null ? end.x : 0, end != null ? end.y : 0);
	}

	/**
	 * Closes the path. If it is closed, Illustrator connects the first and last
	 * segments.
	 */
	public abstract void closePath();

	/**
	 * @deprecated
	 */
	public void quadTo(double handleX, double handleY, double endX, double endY) {
		curveTo(handleX, handleY, endX, endY);
	}

	/**
	 * @deprecated
	 */
	public void quadTo(Point handle, Point end) {
		curveTo(handle.x, handle.y, end.x, end.y);		
	}
	
	/**
	 * @deprecated
	 */
	public void arcTo(double middleX, double middleY, double endX, double endY) {
		arcThrough(middleX, middleY, endX, endY);
	}

	/**
	 * @deprecated
	 */
	public void arcTo(Point middle, Point end) {
		arcThrough(middle.x, middle.y, end.x, end.y);
	}


	/**
	 * @jshide
	 */
	public void append(PathIterator iter) {
		append(iter, false);
	}

	/**
	 * Appends the segments of a Shape to the path. If {@code connect} is
	 * true, the new path segments are connected to the existing one with a
	 * line. The winding rule of the Shape is ignored.
	 * @jshide
	 */
	public void append(Shape shape, boolean connect) {
		append(shape.getPathIterator(null), connect);
	}

	/**
	 * @jshide
	 */
	public void append(Shape shape) {
		append(shape.getPathIterator(null), false);
	}

	/**
	 * {@grouptitle Geometric Tests}
	 * 
	 * Checks if the interior of the path intersects with the interior of the
	 * specified path.
	 * 
	 * @param item
	 * @return {@true if the paths intersect}
	 */
	public boolean intersects(PathItem item) {
		Area area = new Area(this.toShape());
		area.intersect(new Area(item.toShape()));
		return !area.isEmpty();
	}

	/**
	 * Checks if the interior of the path contains the interior of the specified
	 * path.
	 * 
	 * @param item
	 * @return {@true if the path contains the specified path}
	 */
	public boolean contains(PathItem item) {
		Area area = new Area(item.toShape());
		area.subtract(new Area(this.toShape()));
		return area.isEmpty();
	}

	/**
	 * Checks if the specified point is contained within the interior of the path.
	 * 
	 * @param point
	 * @return {@true if the point is contained within the path}
	 */
	public boolean contains(Point point) {
		return new Area(this.toShape()).contains(point.toPoint2D());
	}

	/**
	 * {@grouptitle Boolean Operations}
	 * 
	 * Returns the intersection of the paths as a new path
	 * 
	 * @param item
	 */
	public PathItem intersect(PathItem item) {
		Area area = new Area(this.toShape());
		area.intersect(new Area(item.toShape()));
		CompoundPath compoundPath = new CompoundPath(area);
		compoundPath.setStyle(this.getStyle());
		return compoundPath.simplify();
	}

	/**
	 * Adds the shape of the specified path to the path and returns it as a new
	 * path.
	 * 
	 * @param item
	 */
	public PathItem unite(PathItem item) {
		Area area = new Area(this.toShape());
		area.add(new Area(item.toShape()));
		CompoundPath compoundPath = new CompoundPath(area);
		compoundPath.setStyle(this.getStyle());
		return compoundPath.simplify();
	}

	/**
	 * Subtracts the shape of the specified path from the path and returns it as
	 * a new path.
	 * 
	 * @param item
	 */
	public PathItem exclude(PathItem item) {
		Area area = new Area(this.toShape());
		area.subtract(new Area(item.toShape()));
		CompoundPath compoundPath = new CompoundPath(area);
		compoundPath.setStyle(this.getStyle());
		return compoundPath.simplify();
	}

	/**
	 * Returns all curves contained in the Item. For {@link Path} items this is
	 * the same as {@link Path#getCurves}, for {@link CompoundPath} items it
	 * returns the curves of all the {@link Path} items contained inside.
	 */
	protected abstract List<Curve> getAllCurves();

	/**
	 * Returns all interesections between two {@link Path} items in an array of
	 * {@link CurveLocation} objects. {@link CompoundPath} items are support
	 * too.
	 */
	public CurveLocation[] getIntersections(PathItem path) {
		// First check the bounds of the two paths. If they don't intersect,
		// we don't need to iterate through the whole path.
		if (!getBounds().intersects(path.getBounds()))
			return new CurveLocation[0];
		ArrayList<CurveLocation> locations = new ArrayList<CurveLocation>();
		List<Curve> curves1 = getAllCurves(), curves2 = path.getAllCurves();
		int length1 = curves1.size(), length2 = curves2.size();
		// Convert curves2 to curve values, as we're looping through them for
		// each curve in curves1.
		double[][][] curvesValues2 = new double[length2][][];
		for (int i = 0; i < length2; i++)
			curvesValues2[i] = curves2.get(i).getCurveValues();
		// Now loop through each curve in curves1 and get intersections with
		// the curves in curves2.
		for (int i = 0; i < length1; i++) {
			Curve curve = curves1.get(i);
			double[][] curveValues = curve.getCurveValues();
			for (int j = 0; j < length2; j++)
				Curve.getIntersections(curve, curveValues, curvesValues2[j],
						locations);
		}
		return locations.toArray(new CurveLocation[locations.size()]);
	}
}
