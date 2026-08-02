# Kubernetes Deployment & Manifests — National Assessment Grid

## 1. Overview

NAG provides production-ready Kubernetes manifests and Helm charts located under `deploy/k8s/` for deployment to Minikube, Kind, OpenShift, or cloud Kubernetes clusters (EKS, GKE, AKS).

---

## 2. Minikube / Local K8s Cluster Setup

1. Start local Minikube cluster:
   ```bash
   minikube start --cpus=4 --memory=8192 --driver=docker
   minikube addons enable ingress
   minikube addons enable metrics-server
   ```

2. Apply Kubernetes Namespace & Secrets:
   ```bash
   kubectl apply -f deploy/k8s/namespace.yaml
   kubectl apply -f deploy/k8s/secrets.yaml
   ```

3. Deploy Database & Kafka Dependencies:
   ```bash
   kubectl apply -f deploy/k8s/infrastructure/
   ```

4. Deploy Application Microservices:
   ```bash
   kubectl apply -f deploy/k8s/apps/
   ```

---

## 3. Sample Kubernetes Deployment Manifest

`deploy/k8s/apps/scheduling-service-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scheduling-service
  namespace: nag-core
  labels:
    app: scheduling-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: scheduling-service
  template:
    metadata:
      labels:
        app: scheduling-service
    spec:
      containers:
        - name: scheduling-service
          image: nag/scheduling-service:v1.0.0
          imagePullPolicy: IFNotPresent
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: nag-config
            - secretRef:
                name: nag-secrets
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1024Mi"
              cpu: "1000m"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
```
