/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.part;

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

/**
 * Adapter for making a file resource a suitable input for an editor.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class FileEditorInput implements IFileEditorInput, IPathEditorInput, IURIEditorInput,
		IPersistableElement {
	private IFile file;
	private IPath path;
	private URI uri;

	/**
	 * Creates an editor input based of the given file resource.
	 *
	 * @param file the file resource
	 */
	public FileEditorInput(IFile file) {
		if (file == null) 
			throw new IllegalArgumentException();
		this.file = file;
		
		this.path = internalGetPath();
		if(this.path == null) 
			throw new IllegalArgumentException();
		
		
		this.uri = file.getLocationURI();
		if(this.uri == null)
			throw new IllegalArgumentException();
	}

	/* (non-Javadoc)
	 * Method declared on Object.
	 */
	public int hashCode() {
		return file.hashCode();
	}

	/* (non-Javadoc)
	 * Method declared on Object.
	 *
	 * The <code>FileEditorInput</code> implementation of this <code>Object</code>
	 * method bases the equality of two <code>FileEditorInput</code> objects on the
	 * equality of their underlying <code>IFile</code> resources.
	 */
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IFileEditorInput)) {
			return false;
		}
		IFileEditorInput other = (IFileEditorInput) obj;
		return file.equals(other.getFile());
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	public boolean exists() {
		return file.exists();
	}

	/* (non-Javadoc)
	 * Method declared on IAdaptable.
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IFile.class) {
			return file;
		}
		return file.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * Method declared on IPersistableElement.
	 */
	public String getFactoryId() {
		return FileEditorInputFactory.getFactoryId();
	}

	/* (non-Javadoc)
	 * Method declared on IFileEditorInput.
	 */
	public IFile getFile() {
		return file;
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	public ImageDescriptor getImageDescriptor() {
		IContentType contentType = IDE.getContentType(file);
		return PlatformUI.getWorkbench().getEditorRegistry()
				.getImageDescriptor(file.getName(), contentType);
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	public String getName() {
		return file.getName();
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	public IPersistableElement getPersistable() {
		return this;
	}

	/* (non-Javadoc)
	 * Method declared on IStorageEditorInput.
	 */
	public IStorage getStorage() {
		return file;
	}

	/* (non-Javadoc)
	 * Method declared on IEditorInput.
	 */
	public String getToolTipText() {
		return file.getFullPath().makeRelative().toString();
	}

	/* (non-Javadoc)
	 * Method declared on IPersistableElement.
	 */
	public void saveState(IMemento memento) {
		FileEditorInputFactory.saveState(memento, this);
	}

	

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IURIEditorInput#getURI()
	 */
	public URI getURI() {
		return uri;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPathEditorInput#getPath()
	 */
	public IPath getPath() {
		return this.path;
	}

	/**
	 * Get the path of the receiver.
	 * @return {@link IPath} or <code>null</code> if it cannot be found.
	 */
	private IPath internalGetPath() {
		IPath location = file.getLocation();
		if (location != null)
			return location;
		//this is not a local file, so try to obtain a local file
		try {
	        final URI locationURI = file.getLocationURI();
	        if (locationURI == null)
	           return null;
	        IFileStore store = EFS.getStore(locationURI);
			//first try to obtain a local file directly fo1r this store
			java.io.File localFile = store.toLocalFile(EFS.NONE, null);
			//if no local file is available, obtain a cached file
			if (localFile == null)
				localFile = store.toLocalFile(EFS.CACHE, null);
			if (localFile == null)
				return null;
			return Path.fromOSString(localFile.getAbsolutePath());
		} catch (CoreException e) {
			//this can only happen if the file system is not available for this scheme
			IDEWorkbenchPlugin.log(
					"Failed to obtain file store for resource", e); //$NON-NLS-1$
			throw new RuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getClass().getName() + "(" + getFile().getFullPath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
