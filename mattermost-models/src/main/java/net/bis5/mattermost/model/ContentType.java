/*
 * @(#) net.bis5.mattermost.model.ContentType
 * Copyright (c) 2017 Maruyama Takayuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 */
package net.bis5.mattermost.model;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bis5.mattermost.model.ContentType.ContentTypeDeserializer;
import net.bis5.mattermost.model.serialize.HasCodeSerializer;

/**
 * TODO 型の説明
 * 
 * @author Maruyama Takayuki
 * @since 2017/09/30
 */
@Getter
@RequiredArgsConstructor
@JsonSerialize(using = HasCodeSerializer.class)
@JsonDeserialize(using = ContentTypeDeserializer.class)
public enum ContentType implements HasCode<ContentType> {

	FORM("application/x-www-form-urlencoded"), JSON("application/json");
	private final String code;

	public static ContentType of(String code) {
		return Arrays.stream(values()).filter(c -> c.getCode().equals(code)).findFirst().orElse(null);
	}

	static class ContentTypeDeserializer extends JsonDeserializer<ContentType> {

		/**
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser,
		 *      com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public ContentType deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			String jsonValue = p.getValueAsString();
			return of(jsonValue);
		}
	}

}
