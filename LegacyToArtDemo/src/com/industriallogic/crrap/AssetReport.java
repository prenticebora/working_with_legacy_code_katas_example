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
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

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
		assetReportData.groupTotal = new TreeMap<String, BigDecimal>();
		assetReportData.positions = new TreeMap<String, BigDecimal>();
		assetReportData.totalPositions = new BigDecimal("0.00");
		assetReportData.riskTables = new HashMap<String, BigDecimal>();
		assetReportData.assetToGroup = new HashMap<String, String>();

		assetReportData.totalPositions = calcRisk(records);

		printAssetRisk(writer);
	}

	private void printAssetRisk(PrintWriter writer) {
		writer.write("<groups>\n");
		// groups in sorted order
		Iterator<String> groups = assetReportData.groupTotal.keySet()
				.iterator();
		while (groups.hasNext()) {
			String grp = groups.next();

			BigDecimal position = assetReportData.groupTotal.get(grp);
			BigDecimal product = position.multiply(new BigDecimal(100));
			BigDecimal weight = product.divide(assetReportData.totalPositions,
					2, BigDecimal.ROUND_HALF_UP);
			writer.write("\t<group position='"
					+ position.toPlainString());
			writer.write("' weight='"
					+ weight);
			writer.write("'>\n");
			writer.write("\t\t"
					+ grp + "\n");
			Iterator<String> iter = assetReportData.positions.keySet()
					.iterator();
			boolean notFirstOne = false;
			while (iter.hasNext()) {
				String asset = iter.next();
				// Output asset only if it belongs in group
				if (assetReportData.assetToGroup.get(asset).equalsIgnoreCase(
						grp)) {
					if (notFirstOne)
						writer.write("\n");
					writer.write("\t\t<asset position='"
							+ assetReportData.positions.get(asset)
									.toPlainString() + "' ");
					BigDecimal p = assetReportData.positions.get(asset);
					BigDecimal weight1 = p.multiply(new BigDecimal("100.00"))
							.divide(position, 2, BigDecimal.ROUND_HALF_UP)
							.setScale(2);
					writer.write("weight='"
							+ weight1
							+ "' risk='"
							+ assetReportData.riskTables.get(asset)
									.toPlainString() + "'>\n");
					writer.write("\t\t\t"
							+ asset + "\n");
					writer.write("\t\t</asset>");
					notFirstOne = true;
				}
			}
			writer.write("\n\t</group>\n");
		}
		writer.write("</groups>\n");
		writer.flush();
	}

	private BigDecimal calcRisk(RecordSet records) {
		for (int row = 0; row < records.getRowCount(); row++) {
			BigDecimal positioning = new BigDecimal(1);
			BigDecimal risk = new BigDecimal("0.00");

			String issue = new String("");
			issue = records.getItem(row, "ISSUE_NAME");
			if (records.getItem(row, "ISSUE_FAMILY").toUpperCase()
					.startsWith("FUND")) {
				// pos = quantity * (market-unit price[TERM_TWO])
				BigDecimal perItem = records.getDecimal(row, "MARKET_PRICE")
						.subtract(records.getDecimal(row, "TERM_ONE"));
				positioning = perItem.multiply(
						records.getDecimal(row, "QUANTITY")).setScale(2,
						BigDecimal.ROUND_HALF_UP);
				BigDecimal riskCoefficient = assetReportData.assessor
						.getRiskCoefficient(
								records.getItem(row, "ISSUE_FAMILY"),
								records.getDecimal(row, "TERM_TWO"));
				BigDecimal product = riskCoefficient.multiply(positioning);
				risk = product.divide(new BigDecimal("100.00"), 2,
						BigDecimal.ROUND_HALF_UP);
				assetReportData.positions.put(issue, positioning);
			} else {
				// pos = (quantity * market) - total price[TERM_ONE]
				positioning = records.getDecimal(row, "QUANTITY").multiply(
						records.getDecimal(row, "MARKET_PRICE"));
				positioning = positioning.subtract(
						records.getDecimal(row, "TERM_ONE")).setScale(2,
						BigDecimal.ROUND_HALF_UP);
				BigDecimal product = records.getDecimal(row, "TERM_TWO")
						.multiply(positioning);
				risk = product.divide(new BigDecimal("100.00"), 2,
						BigDecimal.ROUND_HALF_UP);
				assetReportData.positions.put(issue, positioning);
			}
			assetReportData.totalPositions = assetReportData.totalPositions
					.add(assetReportData.positions.get(issue));

			String group = records.getItem(row, "ISSUE_GROUP");
			String name = records.getItem(row, "ISSUE_NAME");
			assetReportData.assetToGroup.put(name, group);
			BigDecimal value = new BigDecimal("0");

			if (assetReportData.groupTotal.containsKey(group))
				value = value.add(assetReportData.groupTotal.get(group))
						.setScale(2);
			value = value.add(assetReportData.positions.get(issue));
			assetReportData.groupTotal.put(group, value.setScale(2));
			assetReportData.riskTables.put(issue, risk);
		}
		return assetReportData.totalPositions;
	}

}
