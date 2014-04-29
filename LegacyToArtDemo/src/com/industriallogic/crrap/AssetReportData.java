package com.industriallogic.crrap;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.TreeMap;

import untouchable.RecordSet;
import untouchable.RiskAssessor;

public class AssetReportData {
	public RiskAssessor assessor;
	public TreeMap<String, BigDecimal> groupTotal;
	public TreeMap<String, BigDecimal> positions;
	public BigDecimal totalPositions;
	public HashMap<String, BigDecimal> riskTables;
	public HashMap<String, String> assetToGroup;

	public AssetReportData() {
	}

	void initAssetReportData() {
		groupTotal = new TreeMap<String, BigDecimal>();
		positions = new TreeMap<String, BigDecimal>();
		totalPositions = new BigDecimal("0.00");
		riskTables = new HashMap<String, BigDecimal>();
		assetToGroup = new HashMap<String, String>();
	}

	BigDecimal calcRisk(RecordSet records) {
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
				BigDecimal riskCoefficient = assessor.getRiskCoefficient(
						records.getItem(row, "ISSUE_FAMILY"),
						records.getDecimal(row, "TERM_TWO"));
				BigDecimal product = riskCoefficient.multiply(positioning);
				risk = product.divide(new BigDecimal("100.00"), 2,
						BigDecimal.ROUND_HALF_UP);
				positions.put(issue, positioning);
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
				positions.put(issue, positioning);
			}
			totalPositions = totalPositions.add(positions.get(issue));

			String group = records.getItem(row, "ISSUE_GROUP");
			String name = records.getItem(row, "ISSUE_NAME");
			assetToGroup.put(name, group);
			BigDecimal value = new BigDecimal("0");

			if (groupTotal.containsKey(group))
				value = value.add(groupTotal.get(group)).setScale(2);
			value = value.add(positions.get(issue));
			groupTotal.put(group, value.setScale(2));
			riskTables.put(issue, risk);
		}
		return totalPositions;
	}
}