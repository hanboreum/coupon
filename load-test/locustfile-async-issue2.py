import random
from locust import task, FastHttpUser
# docker compose up -d --scale worker=3

class CouponTestV1(FastHttpUser):
  connection_timeout = 10.0  #
  network_timeout = 10.0  # 10초만 기다림

  @task
  def issue(self):
    payload = {
      "userId": random.randint(1, 10000000),
      "couponId": 1
    }
    with self.rest("POST", "/v2/issue-async", json=payload):
      pass
