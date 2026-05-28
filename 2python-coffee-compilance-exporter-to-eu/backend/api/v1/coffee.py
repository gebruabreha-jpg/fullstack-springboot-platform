from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from ...crud import coffee as crud_coffee
from ...schemas import coffee as schemas_coffee
from ...db.database import SessionLocal

router = APIRouter()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.post("/exporters/", response_model=schemas_coffee.Exporter)
def create_exporter(exporter: schemas_coffee.ExporterCreate, db: Session = Depends(get_db)):
    return crud_coffee.create_exporter(db, exporter)

@router.get("/exporters/{exporter_id}", response_model=schemas_coffee.Exporter)
def read_exporter(exporter_id: int, db: Session = Depends(get_db)):
    db_exporter = crud_coffee.get_exporter(db, exporter_id)
    if db_exporter is None:
        raise HTTPException(status_code=404, detail="Exporter not found")
    return db_exporter

@router.post("/suppliers/", response_model=schemas_coffee.Supplier)
def create_supplier(supplier: schemas_coffee.SupplierCreate, db: Session = Depends(get_db)):
    return crud_coffee.create_supplier(db, supplier)

@router.get("/suppliers/exporter/{exporter_id}", response_model=List[schemas_coffee.Supplier])
def read_suppliers(exporter_id: int, db: Session = Depends(get_db)):
    return crud_coffee.get_suppliers_by_exporter(db, exporter_id)

@router.post("/farms/", response_model=schemas_coffee.Farm)
def create_farm(farm: schemas_coffee.FarmCreate, db: Session = Depends(get_db)):
    return crud_coffee.create_farm(db, farm)

@router.get("/farms/supplier/{supplier_id}", response_model=List[schemas_coffee.Farm])
def read_farms(supplier_id: int, db: Session = Depends(get_db)):
    return crud_coffee.get_farms_by_supplier(db, supplier_id)

@router.post("/lots/", response_model=schemas_coffee.CoffeeLot)
def create_lot(lot: schemas_coffee.CoffeeLotCreate, db: Session = Depends(get_db)):
    return crud_coffee.create_coffee_lot(db, lot)

@router.get("/lots/exporter/{exporter_id}", response_model=List[schemas_coffee.CoffeeLot])
def read_lots(exporter_id: int, db: Session = Depends(get_db)):
    return crud_coffee.get_lots_by_exporter(db, exporter_id)

@router.get("/lots/{lot_id}/readiness", response_model=int)
def get_lot_readiness(lot_id: int, db: Session = Depends(get_db)):
    return crud_coffee.calculate_readiness_score(db, lot_id)

@router.post("/shipments/", response_model=schemas_coffee.Shipment)
def create_shipment(shipment: schemas_coffee.ShipmentCreate, db: Session = Depends(get_db)):
    return crud_coffee.create_shipment(db, shipment)

@router.post("/documents/", response_model=schemas_coffee.Document)
def upload_document(document: schemas_coffee.DocumentCreate, db: Session = Depends(get_db)):
    return crud_coffee.create_document(db, document)