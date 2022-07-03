import airflow
from airflow import DAG
from datetime import timedelta
from airflow.operators.python_operator import PythonOperator
from airflow.contrib.operators.emr_add_steps_operator import EmrAddStepsOperator
from airflow.contrib.sensors.emr_step_sensor import EmrStepSensor

SPARK_STEPS = [
    {
        'Name': 'iam-emr-spark',
        'ActionOnFailure': "CONTINUE",
        'HadoopJarStep': {
            'Jar': 'command-runner.jar',
            'Args': [
                '/usr/bin/spark-submit',
                '--class', 'Driver.MainApp',
                '--master', 'yarn',
                '--deploy-mode', 'cluster',
                '--num-executors', '2',
                '--driver-memory', '512m',
                '--executor-memory', '3g',
                '--executor-cores', '2',
                's3://iam-midterm-s3/spark-engine_2.12-0.0.1.jar',
                '-p', 'wcd-demo',
                '-i', 'Csv',
                '-s', "{{ task_instance.xcom_pull('parse_request', key='s3location') }}",
                #'-s', 's3://iamaric-python-s3-bucket/banking.csv',
                '-d', 's3://iam-midterm-s3/banking',
                '-c', 'job',
                '-m', 'append',
                '-o', 'parquet',
                '--input-options', 'header=true'
            ]
        }
    }

]

CLUSTER_ID = "j-1ZO9HPZSVU9FD"

DEFAULT_ARGS = {
    'owner': 'wcd_data_engineer',
    'depends_on_past': False,
    'start_date': airflow.utils.dates.days_ago(0),
    'email': ['idrismaric@gmail.com'],
    'email_on_failure': False,
    'e,ail_on_retry': False
}

def retrieve_s3_files(**kwargs):
    s3_location = kwargs['dag_run'].conf['s3_location']
    kwargs['ti'].xcom_push(key = 's3location', value = s3_location)

dag = DAG(
    'emr_job_flow_manual_steps_dag',
    default_args = DEFAULT_ARGS,
    dagrun_timeout = timedelta(hours=2),
    schedule_interval = None
)

parse_request = PythonOperator(task_id = 'parse_request',
                                provide_context = True, # Airflow will pass a set of keyword arguments that can be used in your function
                                python_callable = retrieve_s3_files,
                                dag = dag
                                ) 

step_adder = EmrAddStepsOperator(
    task_id = 'add_steps',
    job_flow_id = CLUSTER_ID,
    aws_conn_id = "aws_default",
    steps = SPARK_STEPS,
    dag = dag
)

step_checker = EmrStepSensor(
    task_id = 'watch_step',
    job_flow_id = CLUSTER_ID,
    step_id = "{{ task_instance.xcom_pull('add_steps', key='return_value')[0] }}",
    aws_conn_id = "aws_default", 
    dag = dag
)

step_adder.set_upstream(parse_request)
step_checker.set_upstream(step_adder)