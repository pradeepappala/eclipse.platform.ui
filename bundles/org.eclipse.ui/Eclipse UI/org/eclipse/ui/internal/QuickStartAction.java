package org.eclipse.ui.internal;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.internal.dialogs.*;
import org.eclipse.ui.internal.model.AdaptableList;
import org.eclipse.ui.internal.registry.WizardsRegistryReader;
import org.eclipse.ui.*;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.help.*;
import org.eclipse.ui.actions.*;
import org.eclipse.swt.widgets.*;

/**
 * Launch the quick start action.
 */
public class QuickStartAction extends PartEventAction {
	private static final String EDITOR_ID = "org.eclipse.ui.internal.dialogs.WelcomeEditor";  //$NON-NLS-1$
	
	private IWorkbench workbench;
	
/**
 *	Create an instance of this class
 */
public QuickStartAction(IWorkbench aWorkbench) {
	super(WorkbenchMessages.getString("QuickStart.text")); //$NON-NLS-1$
	setToolTipText(WorkbenchMessages.getString("QuickStart.toolTip")); //$NON-NLS-1$
	WorkbenchHelp.setHelp(this, IHelpContextIds.QUICK_START_ACTION);
	this.workbench = aWorkbench;
}
/**
 *	The user has invoked this action
 */
public void run() {
	// Ask the user to select a feature
	AboutInfo[] features = ((Workbench)workbench).getFeaturesInfo();
	ArrayList welcomeFeatures = new ArrayList();
	for (int i = 0; i < features.length; i++) {
		if (features[i].getWelcomePageURL() != null) 
			welcomeFeatures.add(features[i]);
	}
	Shell shell = workbench.getActiveWorkbenchWindow().getShell();
	
	if (welcomeFeatures.size() == 0) {
		MessageDialog.openInformation(
			shell, 
			WorkbenchMessages.getString("QuickStartMessageDialog.title"),
			WorkbenchMessages.getString("QuickStartMessageDialog.message"));
		return;
	}			
	
	features = new AboutInfo[welcomeFeatures.size()];
	welcomeFeatures.toArray(features);
	
	// Sort ascending
	Arrays.sort(features, new Comparator() {
		Collator coll = Collator.getInstance(Locale.getDefault());
			public int compare(Object a, Object b) {
				AboutInfo i1, i2;
				String name1, name2;
				i1 = (AboutInfo)a;
				name1 = i1.getFeatureLabel();
				i2 = (AboutInfo)b;
				name2 = i2.getFeatureLabel();
				if (name1 == null)
					name1 = "";
				if (name2 == null)
					name2 = "";
				return coll.compare(name1, name2);
			}
		});
	
	WelcomePageSelectionDialog d = 
		new WelcomePageSelectionDialog(
			workbench.getActiveWorkbenchWindow().getShell(),
			features);
	if(d.open() != d.OK || d.getResult().length != 1)
		return;
		
	AboutInfo feature = (AboutInfo)d.getResult()[0];
	
	// See if the feature wants a specific perspective
	String perspectiveId = feature.getWelcomePerspective();
	if (perspectiveId == null)
		perspectiveId = WorkbenchPlugin.getDefault().getPerspectiveRegistry().getDefaultPerspective();		
			
	WorkbenchPage page;
	try {
		page =
			(WorkbenchPage) workbench.showPerspective(
				perspectiveId,
				workbench.getActiveWorkbenchWindow());
	} catch (WorkbenchException e) {
		return;
	}
	
	page.setEditorAreaVisible(true);

	// create input
	WelcomeEditorInput input = new WelcomeEditorInput(feature);

	// see if we already have a welcome editor
	IEditorPart editor = page.findEditor(input);
	if(editor != null) {
		page.activate(editor);
		return;
	}

	try {
		page.openEditor(input, EDITOR_ID);
	} catch (PartInitException e) {
		IStatus status = new Status(IStatus.ERROR, WorkbenchPlugin.PI_WORKBENCH, 1, WorkbenchMessages.getString("QuickStartAction.openEditorException"), e); //$NON-NLS-1$
		ErrorDialog.openError(
			workbench.getActiveWorkbenchWindow().getShell(),
			WorkbenchMessages.getString("QuickStartAction.errorDialogTitle"),  //$NON-NLS-1$
			WorkbenchMessages.getString("QuickStartAction.errorDialogMessage"),  //$NON-NLS-1$
			status);
	}
}
}
