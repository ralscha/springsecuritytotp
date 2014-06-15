package ch.rasc.sec.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.jpa.domain.AbstractPersistable;

@Entity
@Table(name = "AppRole")
public class Role extends AbstractPersistable<Long> {

	private static final long serialVersionUID = 1L;
	
	@NotNull
	@Size(max = 50)
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}