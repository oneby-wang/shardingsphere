/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy.frontend.command;

import org.apache.shardingsphere.database.protocol.packet.DatabasePacket;
import org.apache.shardingsphere.database.protocol.payload.PacketPayload;
import org.apache.shardingsphere.proxy.frontend.command.executor.QueryCommandExecutor;
import org.apache.shardingsphere.proxy.frontend.command.executor.ResponseType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowLimitedQueryCommandExecutorTest {
    
    @Test
    void assertExecute() throws SQLException {
        QueryCommandExecutor delegate = new TestQueryCommandExecutor(0);
        DatabasePacket expectedDatabasePacket = TestDatabasePacket.INSTANCE;
        assertThat(new RowLimitedQueryCommandExecutor(delegate, 1).execute(), CoreMatchers.is(Collections.singleton(expectedDatabasePacket)));
    }
    
    @Test
    void assertGetResponseType() {
        QueryCommandExecutor delegate = new TestQueryCommandExecutor(0);
        assertThat(new RowLimitedQueryCommandExecutor(delegate, 1).getResponseType(), CoreMatchers.is(ResponseType.QUERY));
    }
    
    @Test
    void assertNextBelowLimit() throws SQLException {
        TestQueryCommandExecutor delegate = new TestQueryCommandExecutor(1);
        RowLimitedQueryCommandExecutor actualQueryCommandExecutor = new RowLimitedQueryCommandExecutor(delegate, 2);
        assertTrue(actualQueryCommandExecutor.next());
        assertFalse(actualQueryCommandExecutor.next());
        assertThat(delegate.nextInvokedCount, CoreMatchers.is(2));
    }
    
    @Test
    void assertNextAtLimit() throws SQLException {
        TestQueryCommandExecutor delegate = new TestQueryCommandExecutor(3);
        RowLimitedQueryCommandExecutor actualQueryCommandExecutor = new RowLimitedQueryCommandExecutor(delegate, 2);
        assertTrue(actualQueryCommandExecutor.next());
        assertTrue(actualQueryCommandExecutor.next());
        assertFalse(actualQueryCommandExecutor.next());
        assertThat(delegate.nextInvokedCount, CoreMatchers.is(2));
    }
    
    @Test
    void assertGetQueryRowPacket() throws SQLException {
        QueryCommandExecutor delegate = new TestQueryCommandExecutor(0);
        DatabasePacket expectedDatabasePacket = TestDatabasePacket.INSTANCE;
        assertThat(new RowLimitedQueryCommandExecutor(delegate, 1).getQueryRowPacket(), CoreMatchers.is(expectedDatabasePacket));
    }
    
    @Test
    void assertClose() throws SQLException {
        TestQueryCommandExecutor delegate = new TestQueryCommandExecutor(0);
        new RowLimitedQueryCommandExecutor(delegate, 1).close();
        assertTrue(delegate.closed);
    }
    
    private static final class TestQueryCommandExecutor implements QueryCommandExecutor {
        
        private final int rowCount;
        
        private int nextInvokedCount;
        
        private boolean closed;
        
        private TestQueryCommandExecutor(final int rowCount) {
            this.rowCount = rowCount;
        }
        
        @Override
        public Collection<DatabasePacket> execute() {
            return Collections.singleton(TestDatabasePacket.INSTANCE);
        }
        
        @Override
        public ResponseType getResponseType() {
            return ResponseType.QUERY;
        }
        
        @Override
        public boolean next() {
            nextInvokedCount++;
            return nextInvokedCount <= rowCount;
        }
        
        @Override
        public DatabasePacket getQueryRowPacket() {
            return TestDatabasePacket.INSTANCE;
        }
        
        @Override
        public void close() {
            closed = true;
        }
    }
    
    private enum TestDatabasePacket implements DatabasePacket {
        
        INSTANCE;
        
        @Override
        public void write(final PacketPayload payload) {
        }
    }
}
