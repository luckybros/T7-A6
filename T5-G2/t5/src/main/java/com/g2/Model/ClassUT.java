package com.g2.Model;

import java.util.List;

public class ClassUT {
	private String name;
	private String date;
	private String difficulty;
	private String codeUri;
	private String description;
	private List<String> category;

	public ClassUT(String name, String date, String difficulty, String codeUri, String description, List<String> category) {
		this.name = name;
		this.date = date;
		this.difficulty = difficulty;
		this.codeUri = codeUri;
		this.description = description;
		this.category = category;
	}

	public ClassUT() {

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}

	public String getCodeUri() {
		return codeUri;
	}

	public void setCodeUri(String codeUri) {
		this.codeUri = codeUri;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getCategory() {
		return category;
	}

	public void setCategory(List<String> category) {
		this.category = category;
	}

	@Override
	public String toString() {
		return "ClassUT{" +
				"name='" + name + '\'' +
				", date='" + date + '\'' +
				", difficulty='" + difficulty + '\'' +
				", codeUri='" + codeUri + '\'' +
				", category=" + category +
				'}';
	}
}
