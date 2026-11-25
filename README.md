# Product Catalogue PoC (Android + Node.js/TypeScript)

A full-stack proof-of-concept product catalogue consisting of a native **Android (Kotlin + Jetpack Compose)** client and a standalone **Node.js + TypeScript (Express)** backend that mediates all product data from the Fake Store API and handles customer enquiries.

## Stack Choice & Architecture
* **Client (`app/`):** Android in Kotlin with Jetpack Compose, `ViewModel` + `StateFlow`, Ktor HTTP Client (`http://10.0.2.2:3000`), and Coil for asynchronous product image rendering. Chosen for native mobile responsiveness, declarative UI state management, and clean separation of UI from networking.
* **Backend (`backend/`):** Node.js with TypeScript and Express. Exposes REST endpoints (`/api/products/*`, `/api/categories/*`, `/api/enquiries`). All upstream calls to `https://fakestoreapi.com` happen strictly inside `FakeStoreRepository` on the backend—the Android client never communicates directly with the Fake Store API.
* **Extensibility:** Both `ProductRepository` and `EmailProvider` are coded against TypeScript interfaces so the Fake Store API and console email logger can be replaced with a production database/CMS and AWS SES without changing routes or the Android client.

## Prerequisites
* **Node.js** (v18+ LTS) and `npm`
* **Android Studio** (Electric Eel / Flamingo or newer) with **JDK 11**
* **Android Emulator** (API 21+)

## Run Locally (< 5 Minutes)

### 1. Start the Backend Server
```bash
cd backend
cp .env.example .env
npm install
npm run dev