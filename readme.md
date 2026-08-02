# NAG – Next-generation Assessment Grid

> **An Open Digital Public Infrastructure (DPI) platform for secure, scalable, AI-ready assessment, entrance examination, certification, and recruitment systems.**

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)]()
[![Status](https://img.shields.io/badge/status-Active%20Development-success.svg)]()

---

# What is NAG?

**Next-generation Assessment Grid (NAG)** is an open-source Digital Public Infrastructure (DPI) platform for building secure, scalable, transparent, and AI-ready assessment ecosystems.

NAG enables governments, universities, certification bodies, enterprises, and educational institutions to design, conduct, evaluate, and audit high-stakes examinations at national scale. Built on modern cloud-native architecture, it supports computer-based testing (CBT), entrance examinations, recruitment, certifications, campus hiring, and continuous assessments.

Designed as a modular, API-first platform, NAG empowers organizations to retain ownership of their assessment infrastructure while avoiding vendor lock-in and benefiting from a collaborative open-source ecosystem.

---

# Vision

NAG (National Assessment Grid) is an open-source Digital Public Infrastructure (DPI) initiative designed to modernize the way governments, universities, certification bodies, and enterprises conduct high-stakes assessments.

Our mission is to build the world's most secure, transparent, scalable, multilingual, AI-ready examination ecosystem that any organization can deploy, extend, and own.

Unlike traditional examination software, NAG is built as a cloud-native, microservices-based platform that emphasizes security, auditability, interoperability, and extensibility.

---

# Why NAG?

Every year millions of candidates participate in:

- Government recruitment examinations
- University entrance examinations
- Professional certifications
- Departmental promotions
- Campus recruitment
- Scholarship examinations

Existing systems often suffer from:

- Vendor lock-in
- Limited scalability
- Security concerns
- Paper leaks
- Limited transparency
- Poor auditability
- Lack of interoperability
- High licensing costs

NAG aims to solve these challenges through an open, community-driven platform built using modern cloud-native technologies.

---

# Objectives

- Build a secure examination ecosystem
- Eliminate vendor lock-in
- Promote transparency
- Support Digital Public Infrastructure initiatives
- Enable sovereign deployments
- Reduce examination fraud
- Support AI-assisted assessments
- Provide enterprise-grade observability
- Encourage community innovation

---

# Key Features

## Examination Management

- Examination lifecycle management
- Multi-phase examinations
- Multiple shifts
- Multiple schedules
- Center allocation
- Candidate allocation
- Scheduling engine

---

## Candidate Management

- Registration
- Profile management
- Reservation support
- Document verification
- Admit cards
- Hall tickets
- Notifications

---

## Question Bank

- Rich question authoring
- Versioning
- Approval workflow
- Metadata
- Difficulty tagging
- Multi-language support
- Reusable question bank

---

## Secure Paper Generation

- Blueprint driven generation
- Difficulty balancing
- Randomization
- Multiple paper sets
- Secure encryption
- Digital signatures
- Time-lock support

---

## Examination Delivery

- Computer Based Test (CBT)
- Offline mode
- Hybrid mode
- Auto save
- Resume support
- Accessibility support

---

## Evaluation

- Automatic evaluation
- Manual evaluation
- Hybrid evaluation
- Normalization
- Percentile calculation
- Merit generation

---

## AI Ready

Future roadmap includes:

- AI-assisted question generation
- AI translation validation
- AI proctoring
- AI anomaly detection
- AI analytics
- AI-powered examination insights

---

# Platform Architecture

NAG follows a cloud-native microservices architecture.

```
                 +-----------------------+
                 |    Web / Mobile UI    |
                 +-----------+-----------+
                             |
                    API Gateway / BFF
                             |
 ---------------------------------------------------------------
| Identity | Candidate | Exam | Question | Schedule | Workflow |
 ---------------------------------------------------------------
| Paper | Delivery | Evaluation | Result | Analytics | Audit |
 ---------------------------------------------------------------
                             |
                      Event Streaming
                           Kafka
                             |
 ---------------------------------------------------------------
| PostgreSQL | Redis | OpenSearch | Object Storage | Vault |
 ---------------------------------------------------------------
                             |
                  Kubernetes / Cloud Platform
```

---

# Core Modules

- Identity & Access Management
- Candidate Management
- Examination Management
- Examination Scheduling
- Question Bank
- Translation Management
- Paper Generation
- Examination Delivery
- Evaluation
- Result Processing
- Notification
- Analytics
- Audit
- Administration

---

# Technology Stack

## Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Cloud
- Spring AI

## Frontend

- Angular
- TypeScript

## Infrastructure

- Kubernetes
- Docker
- Kafka
- PostgreSQL
- Redis
- OpenSearch
- HashiCorp Vault

## Observability

- OpenTelemetry
- Prometheus
- Grafana
- Jaeger

## Security

- OAuth2
- OIDC
- JWT
- Keycloak
- TLS
- Role-Based Access Control (RBAC)

---

# Design Principles

- Security First
- Cloud Native
- API First
- Event Driven
- Zero Trust
- Privacy by Design
- AI Ready
- Open Standards
- Vendor Neutral
- Community Driven

---

# Roadmap

## Phase 1

- Core Platform
- Identity
- Examination Scheduling
- Candidate Management

## Phase 2

- Question Bank
- Workflow Engine
- Paper Generation
- Notifications

## Phase 3

- Secure Delivery
- Evaluation
- Result Processing

## Phase 4

- AI Proctoring
- AI Analytics
- AI Question Generation

## Phase 5

- Internationalization
- Digital Credentials
- Adaptive Assessments

---

# Target Deployments

NAG is designed for:

- National Testing Agencies
- Public Service Commissions
- Government Recruitment Boards
- Universities
- Schools
- Certification Bodies
- Corporate Learning Platforms
- Enterprise Assessments

---

# Contributing

We welcome contributions from developers, architects, educators, security researchers, accessibility experts, and government technology professionals.

Ways to contribute:

- Report issues
- Submit pull requests
- Improve documentation
- Add new modules
- Enhance security
- Build integrations
- Improve accessibility

Please read our CONTRIBUTING.md before submitting changes.

---

# Documentation

Comprehensive documentation is available in the `/docs` directory.

- Architecture
- Deployment Guide
- Developer Guide
- API Reference
- Security Architecture
- Database Design
- Module Specifications
- Roadmap

---

# License

Licensed under the Apache License 2.0.

---

# Vision Statement

> **To build the world's most trusted open Digital Public Infrastructure for secure, scalable, transparent, multilingual, and AI-powered assessment systems.**

---

## Star the Project

If NAG helps your organization or inspires your work, please consider giving the repository a ⭐ and joining our community.