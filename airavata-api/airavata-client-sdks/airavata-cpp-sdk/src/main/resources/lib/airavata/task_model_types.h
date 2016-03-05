/**
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

/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
#ifndef task_model_TYPES_H
#define task_model_TYPES_H

#include <iosfwd>

#include <thrift/Thrift.h>
#include <thrift/TApplicationException.h>
#include <thrift/protocol/TProtocol.h>
#include <thrift/transport/TTransport.h>

#include <thrift/cxxfunctional.h>
#include "airavata_commons_types.h"
#include "compute_resource_model_types.h"
#include "data_movement_models_types.h"
#include "application_io_models_types.h"
#include "status_models_types.h"
#include "job_model_types.h"


namespace apache { namespace airavata { namespace model { namespace task {

struct TaskTypes {
  enum type {
    ENV_SETUP = 0,
    DATA_STAGING = 1,
    JOB_SUBMISSION = 2,
    ENV_CLEANUP = 3,
    MONITORING = 4,
    OUTPUT_FETCHING = 5
  };
};

extern const std::map<int, const char*> _TaskTypes_VALUES_TO_NAMES;

struct DataStageType {
  enum type {
    INPUT = 0,
    OUPUT = 1,
    ARCHIVE_OUTPUT = 2
  };
};

extern const std::map<int, const char*> _DataStageType_VALUES_TO_NAMES;

class TaskModel;

class DataStagingTaskModel;

class EnvironmentSetupTaskModel;

class JobSubmissionTaskModel;

class MonitorTaskModel;

typedef struct _TaskModel__isset {
  _TaskModel__isset() : taskDetail(false), subTaskModel(false), taskError(false), jobs(false) {}
  bool taskDetail :1;
  bool subTaskModel :1;
  bool taskError :1;
  bool jobs :1;
} _TaskModel__isset;

class TaskModel {
 public:

  TaskModel(const TaskModel&);
  TaskModel& operator=(const TaskModel&);
  TaskModel() : taskId("DO_NOT_SET_AT_CLIENTS"), taskType((TaskTypes::type)0), parentProcessId(), creationTime(0), lastUpdateTime(0), taskDetail(), subTaskModel() {
  }

  virtual ~TaskModel() throw();
  std::string taskId;
  TaskTypes::type taskType;
  std::string parentProcessId;
  int64_t creationTime;
  int64_t lastUpdateTime;
   ::apache::airavata::model::status::TaskStatus taskStatus;
  std::string taskDetail;
  std::string subTaskModel;
   ::apache::airavata::model::commons::ErrorModel taskError;
  std::vector< ::apache::airavata::model::job::JobModel>  jobs;

  _TaskModel__isset __isset;

  void __set_taskId(const std::string& val);

  void __set_taskType(const TaskTypes::type val);

  void __set_parentProcessId(const std::string& val);

  void __set_creationTime(const int64_t val);

  void __set_lastUpdateTime(const int64_t val);

  void __set_taskStatus(const  ::apache::airavata::model::status::TaskStatus& val);

  void __set_taskDetail(const std::string& val);

  void __set_subTaskModel(const std::string& val);

  void __set_taskError(const  ::apache::airavata::model::commons::ErrorModel& val);

  void __set_jobs(const std::vector< ::apache::airavata::model::job::JobModel> & val);

  bool operator == (const TaskModel & rhs) const
  {
    if (!(taskId == rhs.taskId))
      return false;
    if (!(taskType == rhs.taskType))
      return false;
    if (!(parentProcessId == rhs.parentProcessId))
      return false;
    if (!(creationTime == rhs.creationTime))
      return false;
    if (!(lastUpdateTime == rhs.lastUpdateTime))
      return false;
    if (!(taskStatus == rhs.taskStatus))
      return false;
    if (__isset.taskDetail != rhs.__isset.taskDetail)
      return false;
    else if (__isset.taskDetail && !(taskDetail == rhs.taskDetail))
      return false;
    if (__isset.subTaskModel != rhs.__isset.subTaskModel)
      return false;
    else if (__isset.subTaskModel && !(subTaskModel == rhs.subTaskModel))
      return false;
    if (__isset.taskError != rhs.__isset.taskError)
      return false;
    else if (__isset.taskError && !(taskError == rhs.taskError))
      return false;
    if (__isset.jobs != rhs.__isset.jobs)
      return false;
    else if (__isset.jobs && !(jobs == rhs.jobs))
      return false;
    return true;
  }
  bool operator != (const TaskModel &rhs) const {
    return !(*this == rhs);
  }

  bool operator < (const TaskModel & ) const;

  uint32_t read(::apache::thrift::protocol::TProtocol* iprot);
  uint32_t write(::apache::thrift::protocol::TProtocol* oprot) const;

  virtual void printTo(std::ostream& out) const;
};

void swap(TaskModel &a, TaskModel &b);

inline std::ostream& operator<<(std::ostream& out, const TaskModel& obj)
{
  obj.printTo(out);
  return out;
}

typedef struct _DataStagingTaskModel__isset {
  _DataStagingTaskModel__isset() : transferStartTime(false), transferEndTime(false), transferRate(false), processInput(false), processOutput(false) {}
  bool transferStartTime :1;
  bool transferEndTime :1;
  bool transferRate :1;
  bool processInput :1;
  bool processOutput :1;
} _DataStagingTaskModel__isset;

class DataStagingTaskModel {
 public:

  DataStagingTaskModel(const DataStagingTaskModel&);
  DataStagingTaskModel& operator=(const DataStagingTaskModel&);
  DataStagingTaskModel() : source(), destination(), type((DataStageType::type)0), transferStartTime(0), transferEndTime(0), transferRate() {
  }

  virtual ~DataStagingTaskModel() throw();
  std::string source;
  std::string destination;
  DataStageType::type type;
  int64_t transferStartTime;
  int64_t transferEndTime;
  std::string transferRate;
   ::apache::airavata::model::application::io::InputDataObjectType processInput;
   ::apache::airavata::model::application::io::OutputDataObjectType processOutput;

  _DataStagingTaskModel__isset __isset;

  void __set_source(const std::string& val);

  void __set_destination(const std::string& val);

  void __set_type(const DataStageType::type val);

  void __set_transferStartTime(const int64_t val);

  void __set_transferEndTime(const int64_t val);

  void __set_transferRate(const std::string& val);

  void __set_processInput(const  ::apache::airavata::model::application::io::InputDataObjectType& val);

  void __set_processOutput(const  ::apache::airavata::model::application::io::OutputDataObjectType& val);

  bool operator == (const DataStagingTaskModel & rhs) const
  {
    if (!(source == rhs.source))
      return false;
    if (!(destination == rhs.destination))
      return false;
    if (!(type == rhs.type))
      return false;
    if (__isset.transferStartTime != rhs.__isset.transferStartTime)
      return false;
    else if (__isset.transferStartTime && !(transferStartTime == rhs.transferStartTime))
      return false;
    if (__isset.transferEndTime != rhs.__isset.transferEndTime)
      return false;
    else if (__isset.transferEndTime && !(transferEndTime == rhs.transferEndTime))
      return false;
    if (__isset.transferRate != rhs.__isset.transferRate)
      return false;
    else if (__isset.transferRate && !(transferRate == rhs.transferRate))
      return false;
    if (__isset.processInput != rhs.__isset.processInput)
      return false;
    else if (__isset.processInput && !(processInput == rhs.processInput))
      return false;
    if (__isset.processOutput != rhs.__isset.processOutput)
      return false;
    else if (__isset.processOutput && !(processOutput == rhs.processOutput))
      return false;
    return true;
  }
  bool operator != (const DataStagingTaskModel &rhs) const {
    return !(*this == rhs);
  }

  bool operator < (const DataStagingTaskModel & ) const;

  uint32_t read(::apache::thrift::protocol::TProtocol* iprot);
  uint32_t write(::apache::thrift::protocol::TProtocol* oprot) const;

  virtual void printTo(std::ostream& out) const;
};

void swap(DataStagingTaskModel &a, DataStagingTaskModel &b);

inline std::ostream& operator<<(std::ostream& out, const DataStagingTaskModel& obj)
{
  obj.printTo(out);
  return out;
}


class EnvironmentSetupTaskModel {
 public:

  EnvironmentSetupTaskModel(const EnvironmentSetupTaskModel&);
  EnvironmentSetupTaskModel& operator=(const EnvironmentSetupTaskModel&);
  EnvironmentSetupTaskModel() : location(), protocol(( ::apache::airavata::model::data::movement::SecurityProtocol::type)0) {
  }

  virtual ~EnvironmentSetupTaskModel() throw();
  std::string location;
   ::apache::airavata::model::data::movement::SecurityProtocol::type protocol;

  void __set_location(const std::string& val);

  void __set_protocol(const  ::apache::airavata::model::data::movement::SecurityProtocol::type val);

  bool operator == (const EnvironmentSetupTaskModel & rhs) const
  {
    if (!(location == rhs.location))
      return false;
    if (!(protocol == rhs.protocol))
      return false;
    return true;
  }
  bool operator != (const EnvironmentSetupTaskModel &rhs) const {
    return !(*this == rhs);
  }

  bool operator < (const EnvironmentSetupTaskModel & ) const;

  uint32_t read(::apache::thrift::protocol::TProtocol* iprot);
  uint32_t write(::apache::thrift::protocol::TProtocol* oprot) const;

  virtual void printTo(std::ostream& out) const;
};

void swap(EnvironmentSetupTaskModel &a, EnvironmentSetupTaskModel &b);

inline std::ostream& operator<<(std::ostream& out, const EnvironmentSetupTaskModel& obj)
{
  obj.printTo(out);
  return out;
}

typedef struct _JobSubmissionTaskModel__isset {
  _JobSubmissionTaskModel__isset() : wallTime(false) {}
  bool wallTime :1;
} _JobSubmissionTaskModel__isset;

class JobSubmissionTaskModel {
 public:

  JobSubmissionTaskModel(const JobSubmissionTaskModel&);
  JobSubmissionTaskModel& operator=(const JobSubmissionTaskModel&);
  JobSubmissionTaskModel() : jobSubmissionProtocol(( ::apache::airavata::model::appcatalog::computeresource::JobSubmissionProtocol::type)0), monitorMode(( ::apache::airavata::model::appcatalog::computeresource::MonitorMode::type)0), wallTime(0) {
  }

  virtual ~JobSubmissionTaskModel() throw();
   ::apache::airavata::model::appcatalog::computeresource::JobSubmissionProtocol::type jobSubmissionProtocol;
   ::apache::airavata::model::appcatalog::computeresource::MonitorMode::type monitorMode;
  int32_t wallTime;

  _JobSubmissionTaskModel__isset __isset;

  void __set_jobSubmissionProtocol(const  ::apache::airavata::model::appcatalog::computeresource::JobSubmissionProtocol::type val);

  void __set_monitorMode(const  ::apache::airavata::model::appcatalog::computeresource::MonitorMode::type val);

  void __set_wallTime(const int32_t val);

  bool operator == (const JobSubmissionTaskModel & rhs) const
  {
    if (!(jobSubmissionProtocol == rhs.jobSubmissionProtocol))
      return false;
    if (!(monitorMode == rhs.monitorMode))
      return false;
    if (__isset.wallTime != rhs.__isset.wallTime)
      return false;
    else if (__isset.wallTime && !(wallTime == rhs.wallTime))
      return false;
    return true;
  }
  bool operator != (const JobSubmissionTaskModel &rhs) const {
    return !(*this == rhs);
  }

  bool operator < (const JobSubmissionTaskModel & ) const;

  uint32_t read(::apache::thrift::protocol::TProtocol* iprot);
  uint32_t write(::apache::thrift::protocol::TProtocol* oprot) const;

  virtual void printTo(std::ostream& out) const;
};

void swap(JobSubmissionTaskModel &a, JobSubmissionTaskModel &b);

inline std::ostream& operator<<(std::ostream& out, const JobSubmissionTaskModel& obj)
{
  obj.printTo(out);
  return out;
}


class MonitorTaskModel {
 public:

  MonitorTaskModel(const MonitorTaskModel&);
  MonitorTaskModel& operator=(const MonitorTaskModel&);
  MonitorTaskModel() : monitorMode(( ::apache::airavata::model::appcatalog::computeresource::MonitorMode::type)0) {
  }

  virtual ~MonitorTaskModel() throw();
   ::apache::airavata::model::appcatalog::computeresource::MonitorMode::type monitorMode;

  void __set_monitorMode(const  ::apache::airavata::model::appcatalog::computeresource::MonitorMode::type val);

  bool operator == (const MonitorTaskModel & rhs) const
  {
    if (!(monitorMode == rhs.monitorMode))
      return false;
    return true;
  }
  bool operator != (const MonitorTaskModel &rhs) const {
    return !(*this == rhs);
  }

  bool operator < (const MonitorTaskModel & ) const;

  uint32_t read(::apache::thrift::protocol::TProtocol* iprot);
  uint32_t write(::apache::thrift::protocol::TProtocol* oprot) const;

  virtual void printTo(std::ostream& out) const;
};

void swap(MonitorTaskModel &a, MonitorTaskModel &b);

inline std::ostream& operator<<(std::ostream& out, const MonitorTaskModel& obj)
{
  obj.printTo(out);
  return out;
}

}}}} // namespace

#endif
