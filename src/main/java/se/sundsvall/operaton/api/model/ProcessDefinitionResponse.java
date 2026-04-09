package se.sundsvall.operaton.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "Process definition response model")
public class ProcessDefinitionResponse {

	@Schema(description = "Process definition ID", examples = "invoice:1:4")
	private String id;

	@Schema(description = "Process definition key", examples = "invoice")
	private String key;

	@Schema(description = "Process definition name", examples = "Invoice Process")
	private String name;

	@Schema(description = "Process definition version", examples = "1")
	private int version;

	public static ProcessDefinitionResponse create() {
		return new ProcessDefinitionResponse();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ProcessDefinitionResponse withId(final String id) {
		this.id = id;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public ProcessDefinitionResponse withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public ProcessDefinitionResponse withName(final String name) {
		this.name = name;
		return this;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(final int version) {
		this.version = version;
	}

	public ProcessDefinitionResponse withVersion(final int version) {
		this.version = version;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ProcessDefinitionResponse that = (ProcessDefinitionResponse) o;
		return version == that.version && Objects.equals(id, that.id) && Objects.equals(key, that.key) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, key, name, version);
	}

	@Override
	public String toString() {
		return "ProcessDefinitionResponse{" +
			"id='" + id + '\'' +
			", key='" + key + '\'' +
			", name='" + name + '\'' +
			", version=" + version +
			'}';
	}
}
