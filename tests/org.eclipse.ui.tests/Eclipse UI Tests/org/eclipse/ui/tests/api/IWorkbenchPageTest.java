/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.tests.api;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.tests.TestPlugin;
import org.eclipse.ui.tests.util.CallHistory;
import org.eclipse.ui.tests.util.EmptyPerspective;
import org.eclipse.ui.tests.util.FileUtil;
import org.eclipse.ui.tests.util.PlatformUtil;
import org.eclipse.ui.tests.util.UITestCase;

public class IWorkbenchPageTest extends UITestCase {

	private IWorkbenchPage fActivePage;
	private IWorkbenchWindow fWin;
	private IProject proj;

	public IWorkbenchPageTest(String testName) {
		super(testName);
	}

	protected void doSetUp() throws Exception {
		super.doSetUp();
		fWin = openTestWindow();
		fActivePage = fWin.getActivePage();
	}

	protected void doTearDown() throws Exception {
		super.doTearDown();
		if (proj != null) {
			try {
				FileUtil.deleteProject(proj);
			} catch (CoreException e) {
				TestPlugin.getDefault().getLog().log(e.getStatus());
				fail();			
				
			}
			proj = null;
		}
	}

	/**
	 *	tests both of the following:	
	 *	setEditorAreaVisible()
	 *	isEditorAreaVisible()
	 */
	public void testGet_SetEditorAreaVisible() throws Throwable {
		fActivePage.setEditorAreaVisible(true);
		assertTrue(fActivePage.isEditorAreaVisible() == true);

		fActivePage.setEditorAreaVisible(false);
		assertTrue(fActivePage.isEditorAreaVisible() == false);
	}

	public void testGetPerspective() throws Throwable {
		assertNotNull(fActivePage.getPerspective());

		IWorkbenchPage page =
			fWin.openPage(
				EmptyPerspective.PERSP_ID,
				ResourcesPlugin.getWorkspace());
		assertEquals(EmptyPerspective.PERSP_ID, page.getPerspective().getId());
	}

	public void testSetPerspective() throws Throwable {
		IPerspectiveDescriptor per =
			PlatformUI
				.getWorkbench()
				.getPerspectiveRegistry()
				.findPerspectiveWithId(
				EmptyPerspective.PERSP_ID);
		fActivePage.setPerspective(per);
		assertEquals(per, fActivePage.getPerspective());
	}

	public void testGetLabel() {
		assertNotNull(fActivePage.getLabel());
	}

	public void testGetInput() throws Throwable {
		IAdaptable input = ResourcesPlugin.getWorkspace();
		IWorkbenchPage page = fWin.openPage(input);
		assertEquals(input, page.getInput());
	}

	public void testActivate() throws Throwable {
		MockViewPart part =
			(MockViewPart) fActivePage.showView(MockViewPart.ID);
		MockViewPart part2 =
			(MockViewPart) fActivePage.showView(MockViewPart.ID2);

		MockPartListener listener = new MockPartListener();
		fActivePage.addPartListener(listener);
		fActivePage.activate(part);

		CallHistory callTrace;

		callTrace = part2.getCallHistory();
		callTrace.clear();
		fActivePage.activate(part2);
		assertTrue(callTrace.contains("setFocus"));
		assertTrue(listener.getCallHistory().contains("partActivated"));

		callTrace = part.getCallHistory();
		callTrace.clear();
		fActivePage.activate(part);
		assertTrue(callTrace.contains("setFocus"));
		assertTrue(listener.getCallHistory().contains("partActivated"));
	}

	public void testBringToTop() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		IEditorPart part = IDE.openEditor(fActivePage, FileUtil.createFile("a.mock1", proj), true);
		IEditorPart part2 = IDE.openEditor(fActivePage, FileUtil.createFile("b.mock1", proj), true);

		MockPartListener listener = new MockPartListener();
		fActivePage.addPartListener(listener);
		CallHistory callTrace = listener.getCallHistory();

		//at this point, part2 is active
		fActivePage.bringToTop(part);
		assertEquals(callTrace.contains("partBroughtToTop"), true);

		callTrace.clear();
		fActivePage.bringToTop(part2);
		assertEquals(callTrace.contains("partBroughtToTop"), true);
	}

	public void testGetWorkbenchWindow() {
		/*
		 * Commented out because until test case can be updated to work
		 * with new window/page/perspective implementation
		 * 
				assertEquals(fActivePage.getWorkbenchWindow(), fWin);
				IWorkbenchPage page = openTestPage(fWin);
				assertEquals(page.getWorkbenchWindow(), fWin);
		*/
	}

	public void testShowView() throws Throwable {
		/*
			javadoc: Shows a view in this page and give it focus
		*/
		MockViewPart view =
			(MockViewPart) fActivePage.showView(MockViewPart.ID);
		assertNotNull(view);
		assertTrue(
			view.getCallHistory().verifyOrder(
				new String[] { "init", "createPartControl", "setFocus" }));

		fActivePage.showView(MockViewPart.ID2);

		/*
			javadoc: If the view is already visible, it is given focus
		*/
		CallHistory callTrace = view.getCallHistory();
		callTrace.clear();
		assertEquals(fActivePage.showView(MockViewPart.ID), view);
		assertEquals(callTrace.contains("setFocus"), true);
	}

	public void testShowViewMult() throws Throwable {
		/*
			javadoc: Shows the view identified by the given view id and secondary id 
			  in this page and gives it focus. 
			  This allows multiple instances of a particular view to be created.  
			  They are disambiguated using the secondary id.
		*/
		MockViewPart view =
			(MockViewPart) fActivePage.showView(MockViewPart.IDMULT);
		assertNotNull(view);
		assertTrue(
			view.getCallHistory().verifyOrder(
				new String[] { "init", "createPartControl", "setFocus" }));
		MockViewPart view2 =
			(MockViewPart) fActivePage.showView(MockViewPart.IDMULT, "2", IWorkbenchPage.VIEW_ACTIVATE);
		assertNotNull(view2);
		assertTrue(
			view2.getCallHistory().verifyOrder(
				new String[] { "init", "createPartControl", "setFocus" }));
		assertTrue(!view.equals(view2));
		MockViewPart view3 =
			(MockViewPart) fActivePage.showView(MockViewPart.IDMULT, "3", IWorkbenchPage.VIEW_ACTIVATE);
		assertNotNull(view3);
		assertTrue(
			view3.getCallHistory().verifyOrder(
				new String[] { "init", "createPartControl", "setFocus" }));
		assertTrue(!view.equals(view3));
		assertTrue(!view2.equals(view3));

		/*
			javadoc: If there is a view identified by the given view id and 
			  secondary id already open in this page, it is given focus.
		*/
		CallHistory callTrace = view.getCallHistory();
		callTrace.clear();
		assertEquals(fActivePage.showView(MockViewPart.IDMULT), view);
		assertEquals(callTrace.contains("setFocus"), true);
		CallHistory callTrace2 = view2.getCallHistory();
		callTrace.clear();
		callTrace2.clear();
		assertEquals(fActivePage.showView(MockViewPart.IDMULT, "2", IWorkbenchPage.VIEW_ACTIVATE), view2);
		assertEquals(callTrace2.contains("setFocus"), true);
		assertEquals(callTrace.contains("setFocus"), false);
		CallHistory callTrace3 = view3.getCallHistory();
		callTrace.clear();
		callTrace2.clear();
		callTrace3.clear();
		assertEquals(fActivePage.showView(MockViewPart.IDMULT, "3", IWorkbenchPage.VIEW_ACTIVATE), view3);
		assertEquals(callTrace3.contains("setFocus"), true);
		assertEquals(callTrace.contains("setFocus"), false);
		assertEquals(callTrace2.contains("setFocus"), false);
		
		/*
		    javadoc: If a secondary id is given, the view must allow multiple instances by
		      having specified allowMultiple="true" in its extension.
		*/
		boolean exceptionThrown = false;
		try {
		    fActivePage.showView(MockViewPart.ID, "2", IWorkbenchPage.VIEW_ACTIVATE);
		}
		catch (PartInitException e) {
		    assertEquals(e.getMessage().indexOf("mult") != -1, true);
		    exceptionThrown = true;
		}
		assertEquals(exceptionThrown, true);
	}

	/**
	 *	openEditor(IFile input)
	 */
	public void testOpenEditor() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");

		/*
			javadoc: 1. The workbench editor registry is consulted to determine if an editor extension has been
			registered for the file type. If so, an instance of the editor extension is opened on the file
		*/
		IFile file = FileUtil.createFile("test.mock1", proj);
		IEditorPart editor = IDE.openEditor(fActivePage, file, true);

		boolean foundEditor = hasEditor(editor);
		assertEquals(foundEditor, true);
		assertEquals(fActivePage.getActiveEditor(), editor);
		assertEquals(
			editor.getSite().getId(),
			fWorkbench.getEditorRegistry().getDefaultEditor(file.getName()).getId());

		/*
			javadoc: 2. Next, the native operating system will be consulted to determine if a native editor exists for 
			the file type. If so, a new process is started and the native editor is opened on the file.
		*/
		//can not be tested

		/*
			javadoc: 3. If all else fails the file will be opened in a default text editor.		
		*/
		
		if (!PlatformUtil.onLinux()) {
			file = FileUtil.createFile("a.null and void", proj);
			editor = IDE.openEditor(fActivePage, file, true);
			assertEquals(hasEditor(editor), true);
			assertEquals(fActivePage.getActiveEditor(), editor);
			assertEquals(
				editor.getSite().getId(),
				"org.eclipse.ui.DefaultTextEditor");
		}

		//open another editor to take the focus away from the first editor
		IDE.openEditor(fActivePage, FileUtil.createFile("test.mock2", proj), true);

		/*	
			javadoc: If this page already has an editor open on the target object that editor is activated
		*/
		//open the editor second time.		
		assertEquals(editor, IDE.openEditor(fActivePage, file, true));
		assertEquals(editor, fActivePage.getActiveEditor());
	}

	/**
	 * openEditor(IFile input, String editorID)
	 */
	public void testOpenEditor2() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		final IFile file = FileUtil.createFile("asfasdasdf", proj);
		final String id = MockEditorPart.ID1;

		/*
			javadoc: The editor type is determined by mapping editorId to an editor extension registered with the workbench.
		*/
		IEditorPart editor = fActivePage.openEditor(new FileEditorInput(file), id);
		assertEquals(editor.getSite().getId(), id);
		assertEquals(hasEditor(editor), true);
		assertEquals(fActivePage.getActiveEditor(), editor);

		//open another editor to take the focus away from the first editor
		IDE.openEditor(fActivePage, FileUtil.createFile("test.mock2", proj), true);

		/*
			javadoc: If this page already has an editor open on the target object that editor is activated
		*/
		//open the first editor second time.
		assertEquals(fActivePage.openEditor(new FileEditorInput(file), id), editor);
		assertEquals(fActivePage.getActiveEditor(), editor);
	}

	/**
	 * openEditor(IEditorInput input,String editorId)                       
	 */
	public void testOpenEditor3() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		final String id = MockEditorPart.ID1;
		IEditorInput input =
			new FileEditorInput(FileUtil.createFile("test.mock1", proj));

		/*
			javadoc: The editor type is determined by mapping editorId to an editor extension registered with the workbench
		*/
		IEditorPart editor = fActivePage.openEditor(input, id);
		assertEquals(editor.getEditorInput(), input);
		assertEquals(editor.getSite().getId(), id);
		assertEquals(hasEditor(editor), true);
		assertEquals(fActivePage.getActiveEditor(), editor);

		//open another editor to take the focus away from the first editor
		IDE.openEditor(fActivePage, FileUtil.createFile("test.mock2", proj), true);

		/*
			javadoc: If this page already has an editor open on the target object that editor is activated
		*/
		//open the first editor second time.
		assertEquals(fActivePage.openEditor(input, id), editor);
		assertEquals(fActivePage.getActiveEditor(), editor);
	}

	/**
	 * openEditor(IEditorInput input, String editorId, boolean activate) 
	 */
	public void testOpenEditor4() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		final String id = MockEditorPart.ID1;
		IEditorInput input =
			new FileEditorInput(FileUtil.createFile("test.mock1", proj));
		MockPartListener listener = new MockPartListener();
		fActivePage.addPartListener(listener);
		CallHistory callTrace = listener.getCallHistory();

		/*
			javadoc: The editor type is determined by mapping editorId to an editor extension 
			registered with the workbench. 
			javadoc: If activate == true the editor will be activated
		*/
		//open an editor with activation
		IEditorPart editor = fActivePage.openEditor(input, id, true);
		assertEquals(editor.getEditorInput(), input);
		assertEquals(editor.getSite().getId(), id);
		assertEquals(hasEditor(editor), true);
		assertEquals(fActivePage.getActiveEditor(), editor);
		assertEquals(callTrace.contains("partActivated"), true);

		//we need another editor so that the editor under test can receive events.
		//otherwise, events will be ignored.
		IEditorPart extra = IDE.openEditor(fActivePage, FileUtil.createFile("aaaaa", proj), true);

		//close the first editor after the second has opened; necessary for
		//test to work with fix to PR 7743
		fActivePage.closeEditor(editor, false);

		//open an editor without activation
		callTrace.clear();
		editor = fActivePage.openEditor(input, id, false);
		assertEquals(editor.getEditorInput(), input);
		assertEquals(editor.getSite().getId(), id);
		assertEquals(hasEditor(editor), true);
		assertEquals(callTrace.contains("partActivated"), false);
		assertEquals(callTrace.contains("partBroughtToTop"), true);

		fActivePage.activate(extra);

		/*
			javadoc: If this page already has an editor open on the target object that editor is brought to the front
		*/
		//open the editor under test second time without activation
		callTrace.clear();
		assertEquals(fActivePage.openEditor(input, id, false), editor);
		assertEquals(callTrace.contains("partBroughtToTop"), true);
		assertEquals(callTrace.contains("partActivated"), false);

		//activate the other editor
		fActivePage.activate(extra);

		/*
			javadoc: If activate == true the editor will be activated
		*/
		//open the editor under test second time with activation
		callTrace.clear();
		assertEquals(fActivePage.openEditor(input, id, true), editor);
		assertEquals(callTrace.contains("partBroughtToTop"), true);
		assertEquals(callTrace.contains("partActivated"), true);
	}

	/**
	 * openEditor(IMarker marker)                       
	 */
	public void testOpenEditor5() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		IMarker marker =
			FileUtil.createFile("aa.mock2", proj).createMarker(IMarker.TASK);
		CallHistory callTrace;

		/*	
			javadoc: the cursor and selection state of the editor is then updated from information 
			recorded in the marker. 
		*/
		//open the registered editor for the marker resource 
		IEditorPart editor = IDE.openEditor(fActivePage, marker, true);
		callTrace = ((MockEditorPart) editor).getCallHistory();
		assertEquals(editor.getSite().getId(), MockEditorPart.ID2);
		assertEquals(hasEditor(editor), true);
		assertEquals(fActivePage.getActiveEditor(), editor);
		assertEquals(callTrace.contains("gotoMarker"), true);
		fActivePage.closeEditor(editor, false);

		/*	
			javadoc: If the marker contains an EDITOR_ID_ATTR attribute the attribute value will be used to 
			determine the editor type to be opened
		*/
		marker.setAttribute(IDE.EDITOR_ID_ATTR, MockEditorPart.ID1);
		editor = IDE.openEditor(fActivePage, marker, true);
		callTrace = ((MockEditorPart) editor).getCallHistory();
		assertEquals(editor.getSite().getId(), MockEditorPart.ID1);
		assertEquals(hasEditor(editor), true);
		assertEquals(fActivePage.getActiveEditor(), editor);
		assertEquals(callTrace.contains("gotoMarker"), true);
		//do not close the editor this time

		/*
			javdoc: If this page already has an editor open on the target object that editor is activated
		*/
		callTrace.clear();
		assertEquals(IDE.openEditor(fActivePage, marker, true), editor);
		assertEquals(fActivePage.getActiveEditor(), editor);
		assertEquals(callTrace.contains("gotoMarker"), true);
		fActivePage.closeEditor(editor, false);
	}

	/**
	 *	openEditor(IMarker marker, boolean activate)                                           
	 */
	public void testOpenEditor6() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		IMarker marker =
			FileUtil.createFile("aa.mock2", proj).createMarker(IMarker.TASK);
		MockPartListener listener = new MockPartListener();
		fActivePage.addPartListener(listener);
		CallHistory listenerCall = listener.getCallHistory();
		CallHistory editorCall;

		//we need another editor so that the editor under test can receive events.
		//otherwise, events will be ignored.
		IEditorPart extra = IDE.openEditor(fActivePage, FileUtil.createFile("aaaaa", proj), true);

		/*
			javadoc: If activate == true the editor will be activated
		*/
		//open the registered editor for the marker resource with activation
		IEditorPart editor = IDE.openEditor(fActivePage, marker, true);
		editorCall = ((MockEditorPart) editor).getCallHistory();
		assertEquals(editor.getSite().getId(), MockEditorPart.ID2);
		assertEquals(hasEditor(editor), true);
		assertEquals(fActivePage.getActiveEditor(), editor);

		/*	
			javadoc: the cursor and selection state of the editor is then updated from information 
			recorded in the marker. 
		*/
		assertEquals(editorCall.contains("gotoMarker"), true);
		fActivePage.closeEditor(editor, false);

		fActivePage.activate(extra);

		//open the registered editor for the marker resource without activation
		listenerCall.clear();
		editor = IDE.openEditor(fActivePage, marker, false);
		editorCall = ((MockEditorPart) editor).getCallHistory();
		assertEquals(editor.getSite().getId(), MockEditorPart.ID2);
		assertEquals(hasEditor(editor), true);
		assertEquals(listenerCall.contains("partBroughtToTop"), true);
		assertEquals(listenerCall.contains("partActivated"), false);
		assertEquals(editorCall.contains("gotoMarker"), true);
		fActivePage.closeEditor(editor, false);

		/*	
			javadoc: If the marker contains an EDITOR_ID_ATTR attribute the attribute value will be used to 
			determine the editor type to be opened
		*/
		String id = MockEditorPart.ID1;
		marker.setAttribute(IDE.EDITOR_ID_ATTR, id);

		//open an editor with activation
		listenerCall.clear();

		editor = IDE.openEditor(fActivePage, marker, true);
		editorCall = ((MockEditorPart) editor).getCallHistory();
		assertEquals(editor.getSite().getId(), id);
		assertEquals(hasEditor(editor), true);
		assertEquals(fActivePage.getActiveEditor(), editor);
		assertEquals(editorCall.contains("gotoMarker"), true);
		fActivePage.closeEditor(editor, false);

		fActivePage.activate(extra);

		//open an editor without activation
		listenerCall.clear();
		editor = IDE.openEditor(fActivePage, marker, false);
		editorCall = ((MockEditorPart) editor).getCallHistory();
		assertEquals(editor.getSite().getId(), id);
		assertEquals(hasEditor(editor), true);
		assertEquals(editorCall.contains("gotoMarker"), true);
		assertEquals(listenerCall.contains("partActivated"), false);
		assertEquals(listenerCall.contains("partBroughtToTop"), true);
		//do not close the editor this time

		fActivePage.activate(extra);

		/*
			javadoc: If this page already has an editor open on the target object that editor is brought to front
		*/
		//open the editor second time without activation
		listenerCall.clear();
		assertEquals(IDE.openEditor(fActivePage, marker, false), editor);
		assertEquals(listenerCall.contains("partBroughtToTop"), true);
		assertEquals(listenerCall.contains("partActivated"), false);

		fActivePage.activate(extra);

		/*
			javdoc: If activate == true the editor will be activated
		*/
		//open the editor second time with activation 		
		listenerCall.clear();
		assertEquals(IDE.openEditor(fActivePage, marker, true), editor);
		assertEquals(editorCall.contains("gotoMarker"), true);
		assertEquals(listenerCall.contains("partBroughtToTop"), true);
		assertEquals(listenerCall.contains("partActivated"), true);
	}

	public void testFindView() throws Throwable {
		String id = MockViewPart.ID3;
		//id of valid, but not open view
		assertNull(fActivePage.findView(id));

		IViewPart view = fActivePage.showView(id);
		assertEquals(fActivePage.findView(id), view);

		//close view		
		fActivePage.hideView(view);
		assertNull(fActivePage.findView(id));
	}
	
	public void testFindViewReference() throws Throwable {
		fActivePage.getWorkbenchWindow().getWorkbench().showPerspective(SessionPerspective.ID, fActivePage.getWorkbenchWindow());
		assertNull(fActivePage.findViewReference(MockViewPart.ID4));
		
		fActivePage.showView(MockViewPart.ID4);
		assertNotNull(fActivePage.findViewReference(MockViewPart.ID4));
	}

	public void testGetViews() throws Throwable {
		int totalBefore = fActivePage.getViewReferences().length;

		IViewPart view = fActivePage.showView(MockViewPart.ID2);
		assertEquals(hasView(view),true);
		assertEquals(fActivePage.getViewReferences().length, totalBefore + 1);

		fActivePage.hideView(view);
		assertEquals(hasView(view),false);
		assertEquals(fActivePage.getViewReferences().length, totalBefore);
	}

	public void testHideViewWithPart() throws Throwable {
		// test that nothing bad happens with a null parameter
		try {
			fActivePage.hideView((IViewPart)null);
		}
		catch (RuntimeException e) {
			fail(e.getMessage());
		}		
		
		IViewPart view = fActivePage.showView(MockViewPart.ID3);

		fActivePage.hideView(view);
		CallHistory callTrace = ((MockViewPart) view).getCallHistory();
		assertTrue(callTrace.contains("dispose"));		
	}
	
	public void testHideViewWithReference() throws Throwable {
		// test that nothing bad happens with a null parameter
		try {
			fActivePage.hideView((IViewReference)null);
		}
		catch (RuntimeException e) {
			fail(e.getMessage());
		}		
		
		IViewPart view = fActivePage.showView(MockViewPart.ID4);
		IViewReference ref = fActivePage.findViewReference(MockViewPart.ID4);
		fActivePage.hideView(ref);
		CallHistory callTrace = ((MockViewPart) view).getCallHistory();
		assertTrue(callTrace.contains("dispose"));
		
		
	}

	public void testClose() throws Throwable {
		IWorkbenchPage page = openTestPage(fWin);

		proj = FileUtil.createProject("testOpenEditor");
		final IFile file = FileUtil.createFile("aaa.mock1", proj);
		IEditorPart editor = IDE.openEditor(page, file, true);
		CallHistory callTrace = ((MockEditorPart) editor).getCallHistory();
		callTrace.clear();

		/*
			javadoc: If the page has open editors with unsaved content and save is true, the user will be given the opportunity to save them
		*/
		assertEquals(page.close(), true);
		assertEquals(
			callTrace.verifyOrder(new String[] { "isDirty", "dispose" }),
			true);
		assertEquals(fWin.getActivePage(), fActivePage);
	}

	public void testCloseEditor() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		final IFile file = FileUtil.createFile("test.mock1", proj);
		IEditorPart editor;
		CallHistory callTrace;
		MockEditorPart mock;

		/*
			javadoc: Parameters: save - true to save the editor contents if required (recommended)
		*/
		//create a clean editor that needs to be saved on closing
		editor = IDE.openEditor(fActivePage, file, true);
		mock = (MockEditorPart) editor;
		mock.setSaveNeeded(true);
		callTrace = mock.getCallHistory();
		callTrace.clear();
		//close the editor with save confirmation
		assertEquals(fActivePage.closeEditor(editor, true), true);
		assertEquals(
			callTrace.verifyOrder(new String[] { "isDirty", "dispose" }),
			true);

		/*
			javadoc: If the editor has unsaved content and save is true, the user will be given the opportunity to save it.
		*/
		//can't be tested

		/*
			javadoc: Parameters: save - false to discard any unsaved changes
		*/
		//create a dirty editor
		editor = IDE.openEditor(fActivePage, file, true);
		mock = (MockEditorPart) editor;
		mock.setDirty(true);
		mock.setSaveNeeded(true);
		callTrace = mock.getCallHistory();
		callTrace.clear();
		//close the editor and discard changes
		assertEquals(fActivePage.closeEditor(editor, false), true);
		assertEquals(callTrace.contains("isSaveOnCloseNeeded"), false);
		/*
		 * It is possible that some action may query the isDirty value of
		 * the editor to update its enabled state. There is nothing wrong
		 * in doing that, so do not test for no isDirty call here.
		 *
		 * assertEquals(callTrace.contains( "isDirty"), false);
		 */
		assertEquals(callTrace.contains("doSave"), false);
		assertEquals(callTrace.contains("dispose"), true);
	}

	public void testCloseEditors() throws Throwable {
		int total = 5;
		final IFile[] files = new IFile[total];
		IEditorPart[] editors = new IEditorPart[total];
		IEditorReference[] editorRefs = new IEditorReference[total];
		CallHistory[] callTraces = new CallHistory[total];
		MockEditorPart[] mocks = new MockEditorPart[total];

		proj = FileUtil.createProject("testCloseEditors");
		for (int i = 0; i < total; i++)
			files[i] = FileUtil.createFile(i + ".mock2", proj);

		/*
			javadoc: If the page has open editors with unsaved content and save is true, the user will be given the opportunity to save them.
		*/
		//close all clean editors with confirmation
		for (int i = 0; i < total; i++) {
			editors[i] = IDE.openEditor(fActivePage, files[i], true);
			callTraces[i] = ((MockEditorPart) editors[i]).getCallHistory();
		}
		
		editorRefs = fActivePage.getEditorReferences();
		assertEquals(fActivePage.closeEditors(editorRefs, true), true);
		for (int i = 0; i < total; i++) {
			assertEquals(callTraces[i].contains("isDirty"), true);
			assertEquals(callTraces[i].contains("doSave"), false);
			callTraces[i].clear();
		}

		//close all dirty editors with confirmation
		//can't be tested		

		//close all dirty editors discarding them		
		for (int i = 0; i < total; i++) {
			editors[i] = IDE.openEditor(fActivePage, files[i], true);
			mocks[i] = (MockEditorPart) editors[i];
			mocks[i].setDirty(true);
			callTraces[i] = mocks[i].getCallHistory();
		}
		editorRefs = fActivePage.getEditorReferences();
		assertEquals(fActivePage.closeEditors(editorRefs, false), true);
		for (int i = 0; i < total; i++) {
			assertEquals(callTraces[i].contains("doSave"), false);
		}
		
		//close empty array of editors
		total = 1;
		for (int i = 0; i < total; i++) {
			editors[i] = IDE.openEditor(fActivePage, files[i], true);
			mocks[i] = (MockEditorPart) editors[i];
			mocks[i].setDirty(true);
			callTraces[i] = mocks[i].getCallHistory();
		}
		// empty array test
		editorRefs = new IEditorReference[0];
		assertEquals(fActivePage.closeEditors(editorRefs, true), true);
		for (int i = 0; i < total; i++) {
				assertEquals(callTraces[i].contains("isDirty"), true);
				assertEquals(callTraces[i].contains("doSave"), false);
				callTraces[i].clear();
		}
		
		// close the last remaining editor, with save=false
		editorRefs = fActivePage.getEditorReferences();
		fActivePage.closeEditors(editorRefs, false);
	}
	
	public void testCloseAllEditors() throws Throwable {
		int total = 5;
		final IFile[] files = new IFile[total];
		IEditorPart[] editors = new IEditorPart[total];
		CallHistory[] callTraces = new CallHistory[total];
		MockEditorPart[] mocks = new MockEditorPart[total];

		proj = FileUtil.createProject("testOpenEditor");
		for (int i = 0; i < total; i++)
			files[i] = FileUtil.createFile(i + ".mock2", proj);

		/*
			javadoc: If the page has open editors with unsaved content and save is true, the user will be given the opportunity to save them.
		*/
		//close all clean editors with confirmation
		for (int i = 0; i < total; i++) {
			editors[i] = IDE.openEditor(fActivePage, files[i], true);
			callTraces[i] = ((MockEditorPart) editors[i]).getCallHistory();
		}
		assertEquals(fActivePage.closeAllEditors(true), true);
		for (int i = 0; i < total; i++) {
			assertEquals(callTraces[i].contains("isDirty"), true);
			assertEquals(callTraces[i].contains("doSave"), false);
			callTraces[i].clear();
		}

		//close all dirty editors with confirmation
		//can't be tested		

		//close all dirty editors discarding them		
		for (int i = 0; i < total; i++) {
			editors[i] = IDE.openEditor(fActivePage, files[i], true);
			mocks[i] = (MockEditorPart) editors[i];
			mocks[i].setDirty(true);
			callTraces[i] = mocks[i].getCallHistory();
		}
		assertEquals(fActivePage.closeAllEditors(false), true);
		for (int i = 0; i < total; i++) {
			assertEquals(callTraces[i].contains("doSave"), false);
		}
	}

	public void testSaveEditor() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		final IFile file = FileUtil.createFile("test.mock1", proj);
		IEditorPart editor;
		CallHistory callTrace;
		MockEditorPart mock;

		//create a clean editor
		editor = IDE.openEditor(fActivePage, file, true);
		mock = (MockEditorPart) editor;
		callTrace = mock.getCallHistory();
		callTrace.clear();

		/*
			javadoc: Saves the contents of the given editor if dirty. 
			If not, this method returns without effect
		*/
		//save the clean editor with confirmation		
		assertEquals(fActivePage.saveEditor(editor, true), true);
		assertEquals(callTrace.contains("isDirty"), true);
		assertEquals(callTrace.contains("doSave"), false);

		/*
			javadoc: If confirm is true the user is prompted to confirm the command. 
		*/
		//can't be tested

		/*
			javadoc: Otherwise, the save happens without prompt. 
		*/
		//save the clean editor without confirmation
		assertEquals(fActivePage.saveEditor(editor, false), true);
		assertEquals(callTrace.contains("isDirty"), true);
		assertEquals(callTrace.contains("doSave"), false);

		//save the dirty editor without confirmation
		mock.setDirty(true);
		callTrace.clear();
		assertEquals(fActivePage.saveEditor(editor, false), true);
		assertEquals(
			callTrace.verifyOrder(new String[] { "isDirty", "doSave" }),
			true);
	}

	public void testSaveAllEditors() throws Throwable {
		int total = 3;

		final IFile[] files = new IFile[total];
		IEditorPart[] editors = new IEditorPart[total];
		CallHistory[] callTraces = new CallHistory[total];
		MockEditorPart[] mocks = new MockEditorPart[total];

		proj = FileUtil.createProject("testOpenEditor");
		for (int i = 0; i < total; i++) {
			files[i] = FileUtil.createFile(i + ".mock2", proj);
			editors[i] = IDE.openEditor(fActivePage, files[i], true);
			mocks[i] = (MockEditorPart) editors[i];
			callTraces[i] = mocks[i].getCallHistory();
		}

		/*
			javadoc: If there are no dirty editors this method returns without effect.
			javadoc: If confirm is true the user is prompted to confirm the command
		*/
		//save all clean editors with confirmation
		assertEquals(fActivePage.saveAllEditors(true), true);
		for (int i = 0; i < total; i++) {
			assertEquals(callTraces[i].contains("isDirty"), true);
			assertEquals(callTraces[i].contains("doSave"), false);
			callTraces[i].clear();
		}

		//save all dirty editors with confirmation can't be tested

		/*
			javadoc: Parameters: confirm - false to save unsaved changes without asking
		*/
		//save all clean editors without confirmation
		assertEquals(fActivePage.saveAllEditors(false), true);
		for (int i = 0; i < total; i++) {
			assertEquals(callTraces[i].contains("isDirty"), true);
			assertEquals(callTraces[i].contains("doSave"), false);
			callTraces[i].clear();
		}

		//save all dirty editors without confirmation
		for (int i = 0; i < total; i++)
			mocks[i].setDirty(true);
		assertEquals(fActivePage.saveAllEditors(false), true);
		for (int i = 0; i < total; i++)
			assertEquals(
				callTraces[i].verifyOrder(new String[] { "isDirty", "doSave" }),
				true);
	}

	public void testGetEditors() throws Throwable {
		proj = FileUtil.createProject("testOpenEditor");
		int totalBefore = fActivePage.getEditorReferences().length;
		int num = 3;
		IEditorPart[] editors = new IEditorPart[num];

		for (int i = 0; i < num; i++) {
			editors[i] = IDE.openEditor(fActivePage, FileUtil.createFile(i + ".mock2", proj), true);
			assertEquals(hasEditor(editors[i]),true);
		}
		assertEquals(fActivePage.getEditorReferences().length, totalBefore + num);

		fActivePage.closeEditor(editors[0], false);
		assertEquals(hasEditor(editors[0]),false);
		assertEquals(fActivePage.getEditorReferences().length, totalBefore + num - 1);

		fActivePage.closeAllEditors(false);
		assertEquals(fActivePage.getEditorReferences().length, 0);
	}

	public void testShowActionSet() {
		String id = MockActionDelegate.ACTION_SET_ID;
		WorkbenchPage page = (WorkbenchPage) fActivePage;

		int totalBefore = page.getActionSets().length;
		fActivePage.showActionSet(id);

		IActionSetDescriptor[] sets =
			((WorkbenchPage) fActivePage).getActionSets();
		boolean found = false;
		for (int i = 0; i < sets.length; i++)
			if (id.equals(sets[i].getId()))
				found = true;
		assertEquals(found, true);

		//check that the method does not add an invalid action set to itself
		id = IConstants.FakeID;
		fActivePage.showActionSet(id);

		sets = ((WorkbenchPage) fActivePage).getActionSets();
		found = false;
		for (int i = 0; i < sets.length; i++)
			if (id.equals(sets[i].getId()))
				found = true;
		assertEquals(found, false);
		assertEquals(page.getActionSets().length, totalBefore + 1);
	}

	public void testHideActionSet() {
		WorkbenchPage page = (WorkbenchPage) fActivePage;
		int totalBefore = page.getActionSets().length;

		String id = MockWorkbenchWindowActionDelegate.SET_ID;
		fActivePage.showActionSet(id);
		assertEquals(page.getActionSets().length, totalBefore + 1);

		fActivePage.hideActionSet(id);
		assertEquals(page.getActionSets().length, totalBefore);

		IActionSetDescriptor[] sets = page.getActionSets();
		boolean found = false;
		for (int i = 0; i < sets.length; i++)
			if (id.equals(sets[i].getId()))
				found = true;
		assertEquals(found, false);
	}

	/**
	 * Return whether or not the editor exists in the current page.
	 * @param editor
	 *  @return boolean
	 */
	private boolean hasEditor(IEditorPart editor) {
		IEditorReference[] references = fActivePage.getEditorReferences();
		for (int i = 0; i < references.length; i++) {
			if (references[i].getEditor(false).equals(editor))
				return true;
		}
		return false;
	}

	/**
	 * Return whether or not the view exists in the current page.
	 * @param editor
	 * @return boolean
	 */
	private boolean hasView(IViewPart view) {
		IViewReference[] references = fActivePage.getViewReferences();
		for (int i = 0; i < references.length; i++) {
			if (references[i].getView(false).equals(view))
				return true;
		}
		return false;
	}
	
	public void testStackOrder() {
		WorkbenchPage page = (WorkbenchPage) fActivePage;
		try {
			IViewPart part1 = page.showView(MockViewPart.ID);
			IViewPart part2 = page.showView(MockViewPart.ID2);
			IViewPart part3 = page.showView(MockViewPart.ID3);
			IViewPart part4 = page.showView(MockViewPart.ID4);
			
			IViewPart [] stack = page.getViewStack(part1);
			assertTrue(stack.length == 4);
			assertTrue(stack[0] == part4);
			assertTrue(stack[1] == part3);
			assertTrue(stack[2] == part2);
			assertTrue(stack[3] == part1);
			
			page.activate(part2);
			stack = page.getViewStack(part1);
			assertTrue(stack.length == 4);
			assertTrue(stack[0] == part2);
			assertTrue(stack[1] == part4);
			assertTrue(stack[2] == part3);
			assertTrue(stack[3] == part1);
			
			page.activate(part1);
			stack = page.getViewStack(part1);
			assertTrue(stack.length == 4);
			assertTrue(stack[0] == part1);
			assertTrue(stack[1] == part2);
			assertTrue(stack[2] == part4);
			assertTrue(stack[3] == part3);

			page.activate(part3);
			stack = page.getViewStack(part1);
			assertTrue(stack.length == 4);
			assertTrue(stack[0] == part3);
			assertTrue(stack[1] == part1);
			assertTrue(stack[2] == part2);
			assertTrue(stack[3] == part4);
		}
		catch(PartInitException e) {
		    fail(e.getMessage());
		}
	}
}