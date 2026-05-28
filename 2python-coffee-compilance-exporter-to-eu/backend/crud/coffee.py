from sqlalchemy.orm import Session
from ..models.coffee import (
    Exporter, Supplier, Farm, CoffeeLot, Shipment, Document, 
    VerificationRecord, Buyer, User, lot_farms
)
from ..schemas.coffee import (
    ExporterCreate, SupplierCreate, FarmCreate, CoffeeLotCreate, 
    ShipmentCreate, DocumentCreate, BuyerCreate, UserCreate
)
from ..core.security import get_password_hash
import uuid

# Exporter CRUD
def create_exporter(db: Session, exporter: ExporterCreate):
    db_exporter = Exporter(**exporter.model_dump())
    db.add(db_exporter)
    db.commit()
    db.refresh(db_exporter)
    return db_exporter

def get_exporter(db: Session, exporter_id: int):
    return db.query(Exporter).filter(Exporter.id == exporter_id, Exporter.deleted_at.is_(None)).first()

def get_exporters(db: Session, skip: int = 0, limit: int = 100):
    return db.query(Exporter).filter(Exporter.deleted_at.is_(None)).offset(skip).limit(limit).all()

# Supplier CRUD
def create_supplier(db: Session, supplier: SupplierCreate):
    db_supplier = Supplier(**supplier.model_dump())
    db.add(db_supplier)
    db.commit()
    db.refresh(db_supplier)
    return db_supplier

def get_supplier(db: Session, supplier_id: int):
    return db.query(Supplier).filter(Supplier.id == supplier_id, Supplier.deleted_at.is_(None)).first()

def get_suppliers_by_exporter(db: Session, exporter_id: int):
    return db.query(Supplier).filter(Supplier.exporter_id == exporter_id, Supplier.deleted_at.is_(None)).all()

# Farm CRUD
def create_farm(db: Session, farm: FarmCreate):
    db_farm = Farm(**farm.model_dump())
    db.add(db_farm)
    db.commit()
    db.refresh(db_farm)
    return db_farm

def get_farms_by_supplier(db: Session, supplier_id: int):
    return db.query(Farm).filter(Farm.supplier_id == supplier_id, Farm.deleted_at.is_(None)).all()

# Coffee Lot CRUD
def create_coffee_lot(db: Session, lot: CoffeeLotCreate, db_farm_ids: list[int] = None):
    lot_data = lot.model_dump()
    db_farm_ids = lot_data.pop('farm_ids', None) or db_farm_ids
    db_lot = CoffeeLot(**lot_data)
    if db_farm_ids:
        from sqlalchemy import select
        farms = db.query(Farm).filter(Farm.id.in_(db_farm_ids)).all()
        db_lot.farms = farms
    db.add(db_lot)
    db.commit()
    db.refresh(db_lot)
    return db_lot

def get_coffee_lot(db: Session, lot_id: int):
    return db.query(CoffeeLot).filter(CoffeeLot.id == lot_id, CoffeeLot.deleted_at.is_(None)).first()

def get_lots_by_exporter(db: Session, exporter_id: int):
    return db.query(CoffeeLot).filter(CoffeeLot.exporter_id == exporter_id, CoffeeLot.deleted_at.is_(None)).all()

def calculate_readiness_score(db: Session, lot_id: int) -> int:
    lot = get_coffee_lot(db, lot_id)
    if not lot:
        return 0
    
    score = 20  # Base score
    
    suppliers = get_suppliers_by_exporter(db, lot.exporter_id)
    farms_total = sum(len(get_farms_by_supplier(db, s.id)) for s in suppliers)
    
    if suppliers:
        score += 20
    if farms_total > 0:
        score += 20
    if any(s.gps_coordinates for s in suppliers):
        score += 20
    if db.query(Document).filter(Document.documentable_type == "lot", Document.documentable_id == lot.id).first():
        score += 20
    
    return min(score, 100)

# Shipment CRUD
def create_shipment(db: Session, shipment: ShipmentCreate):
    db_shipment = Shipment(**shipment.model_dump())
    db.add(db_shipment)
    db.commit()
    db.refresh(db_shipment)
    return db_shipment

def get_shipment(db: Session, shipment_id: int):
    return db.query(Shipment).filter(Shipment.id == shipment_id, Shipment.deleted_at.is_(None)).first()

# Document CRUD
def create_document(db: Session, document: DocumentCreate):
    db_document = Document(**document.model_dump())
    db.add(db_document)
    db.commit()
    db.refresh(db_document)
    return db_document

def get_documents_for_lot(db: Session, lot_id: int):
    return db.query(Document).filter(Document.documentable_type == "lot", Document.documentable_id == lot_id, Document.deleted_at.is_(None)).all()

# User CRUD
def create_user(db: Session, user: UserCreate):
    db_user = User(
        email=user.email,
        password_hash=get_password_hash(user.password),
        exporter_id=user.exporter_id,
        role=user.role
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

def get_user_by_email(db: Session, email: str):
    return db.query(User).filter(User.email == email, User.deleted_at.is_(None)).first()

def authenticate_user(db: Session, email: str, password: str):
    from ..core.security import verify_password
    user = get_user_by_email(db, email)
    if not user or not verify_password(password, user.password_hash):
        return None
    return user

# Buyer CRUD
def create_buyer(db: Session, buyer: BuyerCreate):
    db_buyer = Buyer(
        **buyer.model_dump(),
        access_token=str(uuid.uuid4())
    )
    db.add(db_buyer)
    db.commit()
    db.refresh(db_buyer)
    return db_buyer

def get_buyer_by_token(db: Session, token: str):
    return db.query(Buyer).filter(Buyer.access_token == token, Buyer.deleted_at.is_(None)).first()