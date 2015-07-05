package ch.rasc.sec.entity;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class LocalDateTimeConverter
		implements AttributeConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convertToDatabaseColumn(LocalDateTime value) {
		if (value != null) {
			return Timestamp.valueOf(value);
		}
		return null;
	}

	@Override
	public LocalDateTime convertToEntityAttribute(Timestamp value) {
		if (value != null) {
			return value.toLocalDateTime();
		}
		return null;
	}

}
