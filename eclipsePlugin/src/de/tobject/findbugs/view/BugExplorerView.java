/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.tobject.findbugs.view;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.navigator.CommonNavigator;

import de.tobject.findbugs.FindbugsPlugin;

public class BugExplorerView extends CommonNavigator implements IMarkerSelectionHandler, ISelectionChangedListener {

	private MarkerSelectionListener selectionListener;

	private static final String TAG_MEMENTO = "memento";

	private IMemento viewMemento;

	public BugExplorerView() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		// Add selection listener to detect click in problems view or in tree view
		ISelectionService theService = getSite().getWorkbenchWindow()
				.getSelectionService();
		selectionListener = new MarkerSelectionListener(this);
		theService.addSelectionListener(selectionListener);
		getCommonViewer().addSelectionChangedListener(this);
	}

	public boolean isVisible() {
		return getSite().getPage().isPartVisible(this);
	}

	public void markerSelected(IMarker marker) {
		getCommonViewer().setSelection(new StructuredSelection(marker), true);
	}

	@Override
	public void updateTitle() {
		super.updateTitle();
//		setContentDescription(getTitleToolTip());
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		viewMemento = memento;
		if(memento == null){
			IDialogSettings dialogSettings = FindbugsPlugin.getDefault().getDialogSettings();
			String persistedMemento = dialogSettings.get(TAG_MEMENTO);
			if (persistedMemento != null) {
				try {
					memento= XMLMemento.createReadRoot(new StringReader(persistedMemento));
				} catch (WorkbenchException e) {
					// don't do anything. Simply don't restore the settings
				}
			}
		}
		super.init(site, memento);
	}

	@Override
	public Object getAdapter(Class clazz) {
		Object adapter = super.getAdapter(clazz);
		if(adapter == null && clazz == IMemento.class){
			return viewMemento;
		}
		return adapter;
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
	}

	@Override
	public void dispose() {
		// XXX see https://bugs.eclipse.org/bugs/show_bug.cgi?id=223068
		XMLMemento memento= XMLMemento.createWriteRoot("bugExplorer"); //$NON-NLS-1$
		saveState(memento);
		StringWriter writer= new StringWriter();
		try {
			memento.save(writer);
			IDialogSettings dialogSettings = FindbugsPlugin.getDefault().getDialogSettings();
			dialogSettings.put(TAG_MEMENTO, writer.getBuffer().toString());
		} catch (IOException e) {
			// don't do anything. Simply don't store the settings
		}

		if (selectionListener != null) {
			getSite().getWorkbenchWindow().getSelectionService()
					.removeSelectionListener(selectionListener);
			selectionListener = null;
		}
		super.dispose();
	}

	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event.getSelection();
		if(selection.isEmpty() || selection.size() == 1) {
			setContentDescription("");
		} else {
			setContentDescription(getFrameToolTipText(selection));
		}
	}

}
