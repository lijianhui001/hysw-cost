package net.zlw.cloud.librarian.service;

import net.zlw.cloud.librarian.dao.ThoseResponsibleDao;
import net.zlw.cloud.librarian.model.ThoseResponsible;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Transactional
public class ThoseResponsibleService  {
    @Resource
    private ThoseResponsibleDao thoseResponsibleDao;

    public List<ThoseResponsible> findthoseResponsiblAll() {
        List<ThoseResponsible> thoseResponsibles = thoseResponsibleDao.selectAll();
        return thoseResponsibles;
    }

    public void addPerson(String remeberId) {
        ThoseResponsible thoseResponsible = thoseResponsibleDao.selectByPrimaryKey("1");
        String personnel = thoseResponsible.getPersonnel();
        if (personnel == null){
            thoseResponsible.setPersonnel(remeberId);
            thoseResponsibleDao.updateByPrimaryKeySelective(thoseResponsible);
        }else{
            personnel += ","+remeberId;
            thoseResponsible.setPersonnel(personnel);
            thoseResponsibleDao.updateByPrimaryKeySelective(thoseResponsible);
        }
    }
}
