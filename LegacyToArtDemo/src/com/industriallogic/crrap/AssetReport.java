// ***************************************************************************
// Copyright (c) 2013, Industrial Logic, Inc., All Rights Reserved.
//
// This code is the exclusive property of Industrial Logic, Inc. It may ONLY be
// used by students during Industrial Logic's workshops or by individuals
// who are being coached by Industrial Logic on a project.
//
// This code may NOT be copied or used for any other purpose without the prior
// written consent of Industrial Logic, Inc.
// ****************************************************************************

package com.industriallogic.crrap;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Iterator;

import untouchable.RecordSet;
import untouchable.RiskAssessor;

public class AssetReport {

	private AssetReportData assetReportData = new AssetReportData();

	public AssetReport(RiskAssessor assessor) {
		this.assetReportData.assessor = assessor;
	}

	public AssetReport() {
		this(RiskAssessor.getInstance());
	}

	public void execute(RecordSet records, PrintWriter writer) {
		assetReportData.initAssetReportData();

		BigDecimal totalRiskPositioning = assetReportData.calcRisk(records);
		assetReportData.setTotalPositions(totalRiskPositioning);

		printAssetRisk(writer);
	}

	private void printAssetRisk(PrintWriter writer) {
		writer.write("<groups>\n");
		outputGroupsInSortedOrder(writer);
		writer.write("</groups>\n");

		writer.flush();
	}

	private void outputGroupsInSortedOrder(PrintWriter writer) {
		Iterator<String> groupsIter = assetReportData.groupTotal.keySet()
				.iterator();

		while (groupsIter.hasNext()) {
			String group = groupsIter.next();

			outputGroupTag(writer, group);
		}
	}

	private void outputGroupTag(PrintWriter writer, String group) {
		BigDecimal positioning = assetReportData.groupTotal.get(group);

		outputGroupTagAndProperties(writer, positioning);

		outputGroupContentAndAssetTags(writer, group, positioning);

		closeGroupTag(writer);
	}

	private void outputGroupTagAndProperties(PrintWriter writer,
			BigDecimal positioning) {
		BigDecimal product = positioning.multiply(new BigDecimal(100));
		BigDecimal weight = product.divide(assetReportData.totalPositions, 2,
				BigDecimal.ROUND_HALF_UP);

		writer.write("\t<group position='"
				+ positioning.toPlainString());
		writer.write("' weight='"
				+ weight);
		writer.write("'>\n");
	}

	private void outputGroupContentAndAssetTags(PrintWriter writer, String group,
			BigDecimal positioning) {
		writer.write("\t\t"
				+ group + "\n");
		Iterator<String> iter = assetReportData.positions.keySet().iterator();
		boolean notFirstOne = false;

		while (iter.hasNext()) {
			String asset = iter.next();

			notFirstOne = outputAssetTag(writer, group, positioning,
					notFirstOne, asset);
		}
	}

	private void closeGroupTag(PrintWriter writer) {
		writer.write("\n\t</group>\n");
	}

	private boolean outputAssetTag(PrintWriter writer, String group,
			BigDecimal positioning, boolean notFirstOne, String asset) {
		// Output asset only if it belongs in group
		if (assetReportData.assetToGroup.get(asset).equalsIgnoreCase(group)) {
			if (notFirstOne) {
				writer.write("\n");
			}

			writer.write("\t\t<asset position='"
					+ assetReportData.positions.get(asset).toPlainString()
					+ "' ");
			BigDecimal p = assetReportData.positions.get(asset);
			BigDecimal weight1 = p.multiply(new BigDecimal("100.00"))
					.divide(positioning, 2, BigDecimal.ROUND_HALF_UP)
					.setScale(2);
			writer.write("weight='"
					+ weight1 + "' risk='"
					+ assetReportData.riskTables.get(asset).toPlainString()
					+ "'>\n");
			writer.write("\t\t\t"
					+ asset + "\n");
			writer.write("\t\t</asset>");
			notFirstOne = true;
		}

		return notFirstOne;
	}

}
