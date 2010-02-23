/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.jcr.version.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.webdav.version.report.Report;
import org.apache.jackrabbit.webdav.version.report.ReportType;
import org.apache.jackrabbit.webdav.version.report.ReportInfo;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.security.CurrentUserPrivilegeSetProperty;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.ArrayList;
import java.security.AccessControlException;

/**
 * <code>JcrPrivilegeReport</code>...
 */
public class JcrPrivilegeReport extends AbstractJcrReport {

    private static Logger log = LoggerFactory.getLogger(JcrPrivilegeReport.class);

    private static final String REPORT_NAME = "privileges";

    /**
     * The exportview report type
     */
    public static final ReportType PRIVILEGES_REPORT = ReportType.register(REPORT_NAME, ItemResourceConstants.NAMESPACE, JcrPrivilegeReport.class);

    private static final Privilege[] PRIVS = new Privilege[] {
        ItemResourceConstants.PRIVILEGE_JCR_READ,
        ItemResourceConstants.PRIVILEGE_JCR_ADD_NODE,
        ItemResourceConstants.PRIVILEGE_JCR_SET_PROPERTY,
        ItemResourceConstants.PRIVILEGE_JCR_REMOVE};

    private final MultiStatus ms = new MultiStatus();

    /**
     * Returns {@link #PRIVILEGES_REPORT} report type.
     *
     * @return {@link #PRIVILEGES_REPORT}
     * @see org.apache.jackrabbit.webdav.version.report.Report#getType()
     */
    public ReportType getType() {
        return PRIVILEGES_REPORT;
    }

    /**
     * Always returns <code>true</code>.
     *
     * @return true
     * @see org.apache.jackrabbit.webdav.version.report.Report#isMultiStatusReport()
     */
    public boolean isMultiStatusReport() {
        return true;
    }

    /**
     * @see Report#init(DavResource, ReportInfo)
     */
    @Override
    public void init(DavResource resource, ReportInfo info) throws DavException {
        // delegate basic validation to super class
        super.init(resource, info);
        // make also sure, the info contains a DAV:href child element
        if (!info.containsContentElement(DavConstants.XML_HREF, DavConstants.NAMESPACE)) {
            throw new DavException(DavServletResponse.SC_BAD_REQUEST, "dcr:privileges element must at least contain a single DAV:href child.");
        }
        // immediately build the final multistatus element
        Element hrefElem = info.getContentElement(DavConstants.XML_HREF, DavConstants.NAMESPACE);
        String href = DomUtil.getTextTrim(hrefElem);
        DavResourceLocator resourceLoc = resource.getLocator();
        DavResourceLocator loc = resourceLoc.getFactory().createResourceLocator(resourceLoc.getPrefix(), href);
        // immediately build the final multistatus element
        addResponses(loc);
    }

    /**
     * Creates a Xml document from the generated view.
     *
     * @param document
     * @return Xml element representing the output of the specified view.
     * @see org.apache.jackrabbit.webdav.xml.XmlSerializable#toXml(Document)
     */
    public Element toXml(Document document) {
        return ms.toXml(document);
    }

    private void addResponses(DavResourceLocator locator) {
        String repositoryPath = locator.getRepositoryPath();
        MultiStatusResponse resp = new MultiStatusResponse(locator.getHref(false), null);
        List<Privilege> currentPrivs = new ArrayList<Privilege>();
        for (Privilege priv : PRIVS) {
            try {
                getRepositorySession().checkPermission(repositoryPath, priv.getName());
                currentPrivs.add(priv);
            } catch (AccessControlException e) {
                // ignore
                log.debug(e.toString());
            } catch (RepositoryException e) {
                // ignore
                log.debug(e.toString());
            }
        }
        resp.add(new CurrentUserPrivilegeSetProperty(currentPrivs.toArray(new Privilege[currentPrivs.size()])));
        ms.addResponse(resp);
    }
}