# Coffee Compliance Exporter to EU

Prepare coffee shipments for European buyers in days, not weeks.

## Positioning

**Primary customer:** Ethiopian coffee exporters, cooperatives exporting directly, washing stations that sell internationally

**Value proposition:** Get your coffee export EU-buyer ready with organized traceability, shipment data, and compliance documents in one place.

## Quick Start

```bash
# Start all services
docker-compose up -d

# Frontend: http://localhost:3000
# Backend: http://localhost:8000
# API Docs: http://localhost:8000/docs
```

## MVP Features

### 1. Supplier Onboarding
- Add farmers, cooperatives, washing stations
- GPS coordinates upload
- Harvest period tracking
- Lot ID management
- Farm documentation

### 2. Shipment Readiness Checklist
Shows tangible status for each shipment:
- ✅ Supplier registered
- ✅ Farm GPS uploaded
- ✅ Lot traceability complete
- ⚠ Missing legality document
- ⚠ X farms need verification

**EU Buyer Readiness Score:** Calculated percentage showing compliance level

### 3. Buyer-Ready Export Package
One-click "Share shipment with buyer" creates clean page/PDF containing:
- Lot information
- Source farms with coordinates
- Traceability summary
- Shipment documents
- Downloadable files

### 4. Simple Importer Portal (View Only)
Buyers can verify exporter organization without login-heavy enterprise system.

## Architecture

| Layer | Technology |
|-------|------------|
| Backend | FastAPI (Python) |
| Database | PostgreSQL |
| Frontend | Next.js + TypeScript |
| Storage | S3 / Supabase Storage |
| Hosting | Docker-based (AWS / Render / Fly.io) |

### Why This Stack
- **FastAPI:** Strong ecosystem for traceability logic, geospatial handling, bulk imports, audit logs
- **PostgreSQL:** Perfect for relational data (Farmer → Farm → Lot → Shipment → Buyer)
- **Next.js:** Handles dashboards, portals, and admin tools without rewrites
- **S3 Storage:** Scalable file storage for PDFs, GPS files, certificates

## Project Structure

```
coffee-exporter-eu/
├── backend/
│   ├── api/
│   │   └── v1/
│   │       └── coffee.py      # API endpoints
│   ├── crud/
│   │   └── coffee.py          # Database operations
│   ├── db/
│   │   └── database.py        # Database connection
│   ├── models/
│   │   └── coffee.py          # SQLAlchemy models
│   ├── schemas/
│   │   └── coffee.py          # Pydantic schemas
│   ├── main.py                # FastAPI app entry
│   └── requirements.txt
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── layout.tsx
│   │   │   └── page.tsx
│   │   ├── components/
│   │   │   └── ShipmentChecklist.tsx
│   │   └── lib/
│   └── package.json
├── zdocker/
│   ├── Dockerfile.backend
│   └── init.sql
└── docker-compose.yml
```

## Database Schema

See [docs/DATABASE_ARCHITECTURE.md](docs/DATABASE_ARCHITECTURE.md) for complete production-ready schema.

### Core Entities

**Exporter** - Coffee exporting company
- id, name, email, phone, address
- company_registration, is_active

**Supplier** - Farmer/Cooperative/Washing station
- id, exporter_id (FK), supplier_type
- name, contact_person, gps_coordinates

**Farm** - Coffee farm details
- id, supplier_id (FK), farm_name, farm_code
- boundary_polygon, elevation, variety, harvest_period

**CoffeeLot** - Coffee lot batch
- id, exporter_id (FK), lot_id, name
- origin_region, weight_kg, status, farm_count

**Shipment** - Export shipment
- id, coffee_lot_id (FK), shipment_code
- buyer_name, destination_port, status

**Document** - Compliance documents
- id, documentable_type, documentable_id
- document_type, file_path, original_filename

## API Endpoints

### Exporters
- `POST /api/v1/exporters/` - Create exporter
- `GET /api/v1/exporters/{id}` - Get exporter
- `GET /api/v1/exporters/` - List exporters

### Suppliers
- `POST /api/v1/suppliers/` - Create supplier
- `GET /api/v1/suppliers/exporter/{exporter_id}` - List suppliers by exporter

### Farms
- `POST /api/v1/farms/` - Create farm
- `GET /api/v1/farms/supplier/{supplier_id}` - List farms by supplier

### Coffee Lots
- `POST /api/v1/lots/` - Create coffee lot
- `GET /api/v1/lots/exporter/{exporter_id}` - List lots by exporter
- `GET /api/v1/lots/{lot_id}/readiness` - Get EU buyer readiness score

### Shipments
- `POST /api/v1/shipments/` - Create shipment

### Documents
- `POST /api/v1/documents/` - Upload document

## Development

### Backend Setup
```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

## Validation Roadmap

Before scaling, interview 10 Ethiopian exporters:
1. "What exactly do EU buyers ask you for today?"
2. "How are you managing it now?"

If they answer "Excel + WhatsApp + email" → validated market fit.

## Technical Classification

| Metric | Level | Notes |
|--------|-------|-------|
| CPU Usage | LOW | CSV parsing, GPS validation, PDF generation |
| Memory | LOW | File parsing, validation, report generation |
| Storage | MEDIUM → GROWS | PDFs, GPS files, certificates |
| API Load | MEDIUM | Normal SaaS CRUD traffic |
| Data Integrity | CRITICAL | No missing farm IDs, consistent traceability |

## Scaling Plan

**Stage 1 (0-50 exporters):** Supabase, manual support
**Stage 2 (50-500 exporters):** Optimize queries, caching
**Stage 3 (500+ exporters):** Background jobs, potentially split services

## License

MIT