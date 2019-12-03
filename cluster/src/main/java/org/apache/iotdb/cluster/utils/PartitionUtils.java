/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at      http://www.apache.org/licenses/LICENSE-2.0  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.
 */

package org.apache.iotdb.cluster.utils;

import java.util.Objects;
import org.apache.iotdb.cluster.log.Log;
import org.apache.iotdb.cluster.log.logs.PhysicalPlanLog;
import org.apache.iotdb.cluster.partition.PartitionGroup;
import org.apache.iotdb.cluster.partition.PartitionTable;
import org.apache.iotdb.db.exception.PathErrorException;
import org.apache.iotdb.db.metadata.MManager;
import org.apache.iotdb.db.qp.physical.PhysicalPlan;
import org.apache.iotdb.db.qp.physical.sys.CreateTimeSeriesPlan;
import org.apache.iotdb.db.qp.physical.sys.SetStorageGroupPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartitionUtils {

  private static final Logger logger = LoggerFactory.getLogger(PartitionUtils.class);

  private PartitionUtils() {
    // util class
  }

  public static int calculateLogSocket(Log log, PartitionTable partitionTable) {
    if (log instanceof PhysicalPlanLog) {
      PhysicalPlanLog physicalPlanLog = ((PhysicalPlanLog) log);
      PhysicalPlan plan = physicalPlanLog.getPlan();
      if (plan instanceof CreateTimeSeriesPlan) {
        String storageGroup;
        try {
          storageGroup = MManager.getInstance()
              .getStorageGroupNameByPath(((CreateTimeSeriesPlan) plan).getPath().getFullPath());
          return Math.abs(Objects.hash(storageGroup, 0));
        } catch (PathErrorException e) {
          logger.error("Cannot find the storage group of {}", ((CreateTimeSeriesPlan) plan).getPath());
          return 0;
        }
      }
    }
    return 0;
  }

  public static PartitionGroup partitionPlan(PhysicalPlan plan, PartitionTable partitionTable) {
    if (plan instanceof CreateTimeSeriesPlan) {
      return partitionByPath(((CreateTimeSeriesPlan) plan).getPath().getFullPath(), partitionTable);
    }
    logger.error("Unable to partition plan {}", plan);
    return null;
  }

  private static PartitionGroup partitionByPath(String path, PartitionTable partitionTable) {
    String storageGroup;
    try {
      storageGroup = MManager.getInstance()
          .getStorageGroupNameByPath(path);
    } catch (PathErrorException e) {
      return null;
    }

    return partitionTable.route(storageGroup, 0);
  }
}