/*
 * Copyright 2020-2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.fhir.analytics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.common.io.Resources;
import com.google.fhir.analytics.view.ViewApplicationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JdbcResourceWriterTest {

  @Mock private DataSource dataSourceMock;

  @Mock private Connection connectionMock;

  @Mock private PreparedStatement statementMock;

  @Captor private ArgumentCaptor<Integer> indexCaptor;

  @Captor private ArgumentCaptor<String> idCaptor;

  private String patientResourceStr;

  private IParser parser;

  private static FhirContext fhirContext;

  @BeforeClass
  public static void setupFhirContext() {
    fhirContext = FhirContext.forR4();
  }

  @Before
  public void setup() throws IOException, SQLException {
    patientResourceStr =
        Resources.toString(Resources.getResource("patient.json"), StandardCharsets.UTF_8);
    parser = fhirContext.newJsonParser();
    when(dataSourceMock.getConnection()).thenReturn(connectionMock);
    when(connectionMock.prepareStatement(any(String.class))).thenReturn(statementMock);
  }

  @Test
  public void testWriteResource() throws SQLException, ViewApplicationException {
    JdbcResourceWriter testWriter = new JdbcResourceWriter(dataSourceMock, "", fhirContext);
    testWriter.writeResource((Resource) parser.parseResource(patientResourceStr));
    verify(statementMock, times(4)).setString(any(Integer.class), any(String.class));
    verify(statementMock, times(4)).setString(indexCaptor.capture(), idCaptor.capture());
    assertThat(indexCaptor.getAllValues().get(0), equalTo(1));
    assertThat(idCaptor.getAllValues().get(0), equalTo("my-patient-id"));
    assertThat(indexCaptor.getAllValues().get(2), equalTo(3));
    assertThat(idCaptor.getAllValues().get(2), equalTo("my-patient-id"));
  }

  @Test
  public void testDeleteResource() throws SQLException {
    JdbcResourceWriter jdbcResourceWriter = new JdbcResourceWriter(dataSourceMock, "", fhirContext);
    String resourceType = "Patient";
    String id = "123456";
    jdbcResourceWriter.deleteResourceById(resourceType, id);
    verify(statementMock, times(1)).setString(indexCaptor.capture(), idCaptor.capture());
    assertThat(indexCaptor.getAllValues().get(0), equalTo(1));
    assertThat(idCaptor.getAllValues().get(0), equalTo("123456"));
  }
}
