/**
 * Copyright (c) 2009 Stephen Evanchik
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Stephen Evanchik - initial implementation
 */
package info.evanchik.eclipse.karaf.workbench.ui.views;

import info.evanchik.eclipse.karaf.core.KarafPlatformModel;
import info.evanchik.eclipse.karaf.workbench.KarafWorkbenchActivator;
import info.evanchik.eclipse.karaf.workbench.MBeanProvider;
import info.evanchik.eclipse.karaf.workbench.WorkbenchServiceListener;
import info.evanchik.eclipse.karaf.workbench.WorkbenchServiceManager;
import info.evanchik.eclipse.karaf.workbench.jmx.IJMXTransportRegistry;
import info.evanchik.eclipse.karaf.workbench.jmx.JMXServiceDescriptor;
import info.evanchik.eclipse.karaf.workbench.ui.editor.KarafPlatformEditorInput;
import info.evanchik.eclipse.karaf.workbench.ui.editor.KarafPlatformEditorPart;

import java.util.List;

import javax.management.remote.JMXConnector;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * @author Stephen Evanchik (evanchsa@gmail.com)
 *
 */
public class JmxServersView extends ViewPart {

    /**
	 *
	 * @author Stephen Evanchik (evanchsa@gmail.com)
	 *
	 */
	private final class JMXServiceDescriptorContentProvider
		implements IStructuredContentProvider, WorkbenchServiceListener<MBeanProvider>
	{
		@Override
        public void dispose() {
			// What to do?
		}

		@Override
        public Object[] getElements(final Object element) {
		    if (element == mbeanProviderManager) {
	            final List<MBeanProvider> jmxServiceDescriptors = mbeanProviderManager.getServices();

	            return jmxServiceDescriptors.toArray();
		    } else {
		        return null;
		    }
		}

		@Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		}

		@Override
        public void serviceAdded(final MBeanProvider mbeanProvider) {
			viewer.getControl().getDisplay().syncExec(new Runnable() {
				@Override
                public void run() {
					viewer.add(mbeanProvider);
				}
			});
		}

		@Override
        public void serviceRemoved(final MBeanProvider mbeanProvider) {
			viewer.getControl().getDisplay().syncExec(new Runnable() {
				@Override
                public void run() {
					viewer.remove(mbeanProvider);
				}
			});
		}
	}

	/**
	 *
	 * @author Stephen Evanchik (evanchsa@gmail.com)
	 *
	 */
	private final class JMXServiceDescriptorLabelProvider extends LabelProvider {

		@Override
		public Image getImage(final Object element) {
		    if (element instanceof MBeanProvider) {
		        return KarafWorkbenchActivator.getDefault().getImageRegistry().get(KarafWorkbenchActivator.LOGO_16X16_IMG);
		    } else {
		        return null;
		    }
		}

		@Override
		public String getText(final Object element) {
		    if (element instanceof MBeanProvider) {
		        final MBeanProvider mbeanProvider = (MBeanProvider) element;
    			final JMXServiceDescriptor jmxServiceDescriptor = mbeanProvider.getJMXServiceDescriptor();
    			return jmxServiceDescriptor.getName();
		    } else {
		        return null;
		    }
		}
	}

	public static final String VIEW_ID = "info.evanchik.eclipse.karaf.workbench.jmx.serversView";

	private IJMXTransportRegistry jmxTransportRegistry;

	private WorkbenchServiceManager<MBeanProvider> mbeanProviderManager;

	private ListViewer viewer;

	public JmxServersView() {
	    super();

	    jmxTransportRegistry = KarafWorkbenchActivator.getDefault().getJMXTransportRegistry();
	    mbeanProviderManager = KarafWorkbenchActivator.getDefault().getMBeanProviderManager();
    }

	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new FillLayout());

		final JMXServiceDescriptorContentProvider contentProvider =
			new JMXServiceDescriptorContentProvider();

		viewer = new ListViewer(parent, SWT.SINGLE);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new JMXServiceDescriptorLabelProvider());
		viewer.setInput(mbeanProviderManager);

		mbeanProviderManager.addListener(contentProvider);

		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
            public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection =
					(IStructuredSelection) event.getSelection();

				final MBeanProvider mbeanProvider = (MBeanProvider) selection.getFirstElement();
				final JMXServiceDescriptor jmxServiceDescriptor = mbeanProvider.getJMXServiceDescriptor();

                final KarafPlatformModel karafPlatform =
                    (KarafPlatformModel) jmxServiceDescriptor.getAdapter(KarafPlatformModel.class);
				if (karafPlatform != null) {
				    final IEditorInput editorInput = new KarafPlatformEditorInput(karafPlatform, mbeanProvider);
				    try {
                        getSite().getWorkbenchWindow().getActivePage().openEditor(editorInput, KarafPlatformEditorPart.ID);
                    } catch (final PartInitException e) {
                        // TODO: Handle PartInitException
                    }
				} else {
    				final JMXConnector connector =
    					jmxTransportRegistry.getJMXConnector(jmxServiceDescriptor);
				}
			}
		});

		initContextMenu();

		jmxTransportRegistry.loadTransportExtensions();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void setJmxTransportRegistry(final IJMXTransportRegistry jmxTransportRegistry) {
        this.jmxTransportRegistry = jmxTransportRegistry;
    }

	public void setMbeanProviderManager(final WorkbenchServiceManager<MBeanProvider> mbeanProviderManager) {
        this.mbeanProviderManager = mbeanProviderManager;
    }

	protected void initContextMenu() {
        final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                menuMgr.add(new Separator());
                menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });

        final Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }
}
