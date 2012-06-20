package com.phresco.pom.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import com.phresco.pom.model.ReportPlugin;
import com.phresco.pom.site.Reports;

/**
 * @author suresh_ma
 *
 */
public class SiteConfigurator {

	/**
	 * @param constants
	 */
	private void addReportPlugin(Reports constants, File file) {
		try {
			PomProcessor processor = new PomProcessor(file);
			ReportPlugin reportPlugin = new ReportPlugin();
			reportPlugin.setGroupId(constants.getGroupId());
			reportPlugin.setArtifactId(constants.getArtifactId());
			reportPlugin.setVersion(constants.getVersion());
			processor.siteConfig(reportPlugin);
			processor.save();
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param constants
	 */
	public void addReportPlugin(List<Reports> constants,File file){
		for (Reports siteConstants : constants) {
			addReportPlugin(siteConstants,file);
		}
	}
	
	/**
	 * @param constants
	 */
	public void removeReportPlugin(List<Reports> constants,File file){
		
		try {
			PomProcessor processor = new PomProcessor(file);
			for (Reports siteConstants : constants) {
				String groupId = siteConstants.getGroupId();
				String artifactId = siteConstants.getArtifactId();
				processor.removeSitePlugin(groupId,artifactId);
				processor.save();
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
